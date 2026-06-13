package com.loganalytics.engine;

import com.loganalytics.model.LogEntry;
import com.loganalytics.model.ThreatAlert;
import com.loganalytics.repository.LogEntryRepository;
import com.loganalytics.repository.ThreatAlertRepository;
import com.loganalytics.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class ThreatDetectionEngine {

    private static final int BRUTE_FORCE_THRESHOLD = 5;
    private static final int BRUTE_FORCE_WINDOW_MINUTES = 5;
    private static final int RATE_LIMIT_THRESHOLD = 50;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 1;

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union\\s+select|drop\\s+table|insert\\s+into|delete\\s+from|" +
        "select\\s+\\*|exec\\s*\\(|execute\\s*\\(|xp_cmdshell|information_schema|" +
        "or\\s+1\\s*=\\s*1|or\\s+'1'\\s*=\\s*'1|--\\s*$|;\\s*drop|;\\s*delete)"
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|javascript:|onerror=|onload=|eval\\(|alert\\(|document\\.cookie|<iframe|<img\\s+src=)"
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%252e%252e%252f)"
    );

    private final Map<String, Integer> ipRequestCount = new ConcurrentHashMap<>();

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private ThreatAlertRepository threatAlertRepository;

    public void analyzeLog(LogEntry logEntry) {
        detectBruteForce(logEntry);
        detectSqlInjection(logEntry);
        detectXss(logEntry);
        detectPathTraversal(logEntry);
        trackRequestRate(logEntry);
    }

    private void detectBruteForce(LogEntry logEntry) {
        if (!"LOGIN_FAILED".equals(logEntry.getAction())) return;

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(BRUTE_FORCE_WINDOW_MINUTES);
        List<LogEntry> recentFailures = logEntryRepository.findByIpAddressAndActionAndTimestampAfter(
            logEntry.getIpAddress(), "LOGIN_FAILED", windowStart
        );

        if (recentFailures.size() >= BRUTE_FORCE_THRESHOLD) {
            Optional<ThreatAlert> existing = threatAlertRepository
                .findByThreatTypeAndIpAddressAndResolvedFalse("BRUTE_FORCE", logEntry.getIpAddress());

            if (existing.isPresent()) {
                ThreatAlert alert = existing.get();
                alert.setOccurrenceCount(alert.getOccurrenceCount() + 1);
                threatAlertRepository.save(alert);
            } else {
                ThreatAlert alert = new ThreatAlert(
                    "BRUTE_FORCE",
                    "HIGH",
                    logEntry.getIpAddress(),
                    logEntry.getUsername(),
                    String.format("Brute force attack detected: %d failed login attempts from IP %s within %d minutes targeting user '%s'",
                        recentFailures.size(), logEntry.getIpAddress(), BRUTE_FORCE_WINDOW_MINUTES, logEntry.getUsername()),
                    "/login"
                );
                threatAlertRepository.save(alert);
            }
        }
    }

    private void detectSqlInjection(LogEntry logEntry) {
        String details = logEntry.getDetails() != null ? logEntry.getDetails() : "";
        String endpoint = logEntry.getEndpoint() != null ? logEntry.getEndpoint() : "";

        if (SQL_INJECTION_PATTERN.matcher(details).find() || SQL_INJECTION_PATTERN.matcher(endpoint).find()) {
            Optional<ThreatAlert> existing = threatAlertRepository
                .findByThreatTypeAndIpAddressAndResolvedFalse("SQL_INJECTION", logEntry.getIpAddress());

            if (existing.isPresent()) {
                ThreatAlert alert = existing.get();
                alert.setOccurrenceCount(alert.getOccurrenceCount() + 1);
                threatAlertRepository.save(alert);
            } else {
                ThreatAlert alert = new ThreatAlert(
                    "SQL_INJECTION",
                    "CRITICAL",
                    logEntry.getIpAddress(),
                    logEntry.getUsername(),
                    String.format("SQL Injection attempt detected from IP %s at endpoint '%s'. Payload: %s",
                        logEntry.getIpAddress(), endpoint, details.substring(0, Math.min(details.length(), 200))),
                    logEntry.getEndpoint()
                );
                threatAlertRepository.save(alert);
            }
        }
    }

    private void detectXss(LogEntry logEntry) {
        String details = logEntry.getDetails() != null ? logEntry.getDetails() : "";
        if (XSS_PATTERN.matcher(details).find()) {
            ThreatAlert alert = new ThreatAlert(
                "XSS_ATTACK",
                "HIGH",
                logEntry.getIpAddress(),
                logEntry.getUsername(),
                String.format("Cross-Site Scripting (XSS) attempt detected from IP %s. Malicious script payload found in request to '%s'",
                    logEntry.getIpAddress(), logEntry.getEndpoint()),
                logEntry.getEndpoint()
            );
            threatAlertRepository.save(alert);
        }
    }

    private void detectPathTraversal(LogEntry logEntry) {
        String endpoint = logEntry.getEndpoint() != null ? logEntry.getEndpoint() : "";
        String details = logEntry.getDetails() != null ? logEntry.getDetails() : "";

        if (PATH_TRAVERSAL_PATTERN.matcher(endpoint).find() || PATH_TRAVERSAL_PATTERN.matcher(details).find()) {
            ThreatAlert alert = new ThreatAlert(
                "PATH_TRAVERSAL",
                "HIGH",
                logEntry.getIpAddress(),
                logEntry.getUsername(),
                String.format("Directory traversal attack detected from IP %s. Suspicious path pattern found: %s",
                    logEntry.getIpAddress(), endpoint),
                logEntry.getEndpoint()
            );
            threatAlertRepository.save(alert);
        }
    }

    private void trackRequestRate(LogEntry logEntry) {
        String ip = logEntry.getIpAddress();
        ipRequestCount.merge(ip, 1, Integer::sum);
    }

    @Scheduled(fixedRate = 60000)
    public void checkRateLimits() {
        ipRequestCount.forEach((ip, count) -> {
            if (count >= RATE_LIMIT_THRESHOLD) {
                Optional<ThreatAlert> existing = threatAlertRepository
                    .findByThreatTypeAndIpAddressAndResolvedFalse("DDOS_ATTEMPT", ip);

                if (existing.isEmpty()) {
                    ThreatAlert alert = new ThreatAlert(
                        "DDOS_ATTEMPT",
                        "CRITICAL",
                        ip,
                        "UNKNOWN",
                        String.format("Potential DDoS attack detected: %d requests from IP %s within 1 minute (threshold: %d)",
                            count, ip, RATE_LIMIT_THRESHOLD),
                        "/*"
                    );
                    threatAlertRepository.save(alert);
                }
            }
        });
        ipRequestCount.clear();
    }

    @Scheduled(fixedRate = 300000)
    public void detectAnomalousPatterns() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Object[]> topIps = logEntryRepository.topIpAddressesSince(fiveMinutesAgo);

        for (Object[] row : topIps) {
            String ip = (String) row[0];
            Long count = (Long) row[1];

            if (count > 30) {
                Optional<ThreatAlert> existing = threatAlertRepository
                    .findByThreatTypeAndIpAddressAndResolvedFalse("ANOMALOUS_TRAFFIC", ip);

                if (existing.isEmpty()) {
                    ThreatAlert alert = new ThreatAlert(
                        "ANOMALOUS_TRAFFIC",
                        "MEDIUM",
                        ip,
                        "UNKNOWN",
                        String.format("Anomalous traffic pattern detected: %d requests from IP %s in the last 5 minutes",
                            count, ip),
                        "/*"
                    );
                    threatAlertRepository.save(alert);
                }
            }
        }
    }
}
