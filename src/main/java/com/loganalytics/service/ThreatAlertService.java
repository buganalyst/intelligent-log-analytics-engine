package com.loganalytics.service;

import com.loganalytics.model.ThreatAlert;
import com.loganalytics.repository.ThreatAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ThreatAlertService {

    @Autowired
    private ThreatAlertRepository threatAlertRepository;

    public List<ThreatAlert> getAllAlerts() {
        return threatAlertRepository.findAllByOrderByDetectedAtDesc();
    }

    public List<ThreatAlert> getActiveAlerts() {
        return threatAlertRepository.findByResolvedFalseOrderByDetectedAtDesc();
    }

    public List<ThreatAlert> getAlertsBySeverity(String severity) {
        return threatAlertRepository.findBySeverityOrderByDetectedAtDesc(severity);
    }

    public Optional<ThreatAlert> resolveAlert(Long id) {
        Optional<ThreatAlert> alertOpt = threatAlertRepository.findById(id);
        if (alertOpt.isPresent()) {
            ThreatAlert alert = alertOpt.get();
            alert.setResolved(true);
            return Optional.of(threatAlertRepository.save(alert));
        }
        return Optional.empty();
    }

    public Map<String, Object> getThreatSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAlerts", threatAlertRepository.count());
        summary.put("activeAlerts", threatAlertRepository.countByResolvedFalse());
        summary.put("criticalAlerts", threatAlertRepository.countBySeverityAndResolvedFalse("CRITICAL"));
        summary.put("highAlerts", threatAlertRepository.countBySeverityAndResolvedFalse("HIGH"));
        summary.put("mediumAlerts", threatAlertRepository.countBySeverityAndResolvedFalse("MEDIUM"));

        List<Object[]> byType = threatAlertRepository.countByThreatType();
        Map<String, Long> typeMap = new HashMap<>();
        for (Object[] row : byType) {
            typeMap.put((String) row[0], (Long) row[1]);
        }
        summary.put("alertsByType", typeMap);

        List<Object[]> bySeverity = threatAlertRepository.countBySeverity();
        Map<String, Long> severityMap = new HashMap<>();
        for (Object[] row : bySeverity) {
            severityMap.put((String) row[0], (Long) row[1]);
        }
        summary.put("alertsBySeverity", severityMap);

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        summary.put("alertsLast24h", threatAlertRepository.countSince(oneDayAgo));

        return summary;
    }
}
