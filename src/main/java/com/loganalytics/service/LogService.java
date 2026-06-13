package com.loganalytics.service;

import com.loganalytics.model.LogEntry;
import com.loganalytics.repository.LogEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogService {

    @Autowired
    private LogEntryRepository logEntryRepository;

    public LogEntry saveLog(LogEntry entry) {
        return logEntryRepository.save(entry);
    }

    public LogEntry createLog(String logType, String ipAddress, String username,
                               String action, String details, String status,
                               String userAgent, String endpoint, Integer responseCode, Long responseTimeMs) {
        LogEntry entry = new LogEntry(logType, ipAddress, username, action,
                details, status, userAgent, endpoint, responseCode, responseTimeMs);
        return logEntryRepository.save(entry);
    }

    public List<LogEntry> getAllLogs() {
        return logEntryRepository.findAllByOrderByTimestampDesc();
    }

    public List<LogEntry> getRecentLogs() {
        return logEntryRepository.findTop100ByOrderByTimestampDesc();
    }

    public List<LogEntry> getLogsByType(String logType) {
        return logEntryRepository.findByLogTypeOrderByTimestampDesc(logType);
    }

    public List<LogEntry> getFailedLoginAttempts(String ipAddress, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return logEntryRepository.findByIpAddressAndActionAndTimestampAfter(ipAddress, "LOGIN_FAILED", since);
    }

    public List<LogEntry> getRecentLogsFromIp(String ipAddress, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return logEntryRepository.findByIpAddressAndTimestampAfter(ipAddress, since);
    }

    public List<LogEntry> getSqlInjectionAttempts() {
        return logEntryRepository.findByDetailsContaining("SQL_INJECTION");
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalLogs = logEntryRepository.count();
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        stats.put("totalLogs", totalLogs);
        stats.put("logsLastHour", logEntryRepository.countSince(oneHourAgo));
        stats.put("logsLastDay", logEntryRepository.countSince(oneDayAgo));
        stats.put("failedLoginsLastHour", logEntryRepository.countByStatusAndTimestampAfter("FAILED", oneHourAgo));

        List<Object[]> typeBreakdown = logEntryRepository.countByLogType();
        Map<String, Long> typeMap = new HashMap<>();
        for (Object[] row : typeBreakdown) {
            typeMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("logsByType", typeMap);

        List<Object[]> statusBreakdown = logEntryRepository.countByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusBreakdown) {
            statusMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("logsByStatus", statusMap);

        LocalDateTime thirtyMinsAgo = LocalDateTime.now().minusMinutes(30);
        List<Object[]> topIps = logEntryRepository.topIpAddressesSince(thirtyMinsAgo);
        stats.put("topIpAddresses", topIps);

        return stats;
    }
}
