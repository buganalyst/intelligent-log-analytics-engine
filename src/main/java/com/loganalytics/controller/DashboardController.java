package com.loganalytics.controller;

import com.loganalytics.dto.ApiResponse;
import com.loganalytics.model.ThreatAlert;
import com.loganalytics.service.LogService;
import com.loganalytics.service.ThreatAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/svc/dashboard")
public class DashboardController {

    @Autowired
    private LogService logService;

    @Autowired
    private ThreatAlertService threatAlertService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = logService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok("Stats fetched", stats));
    }

    @GetMapping("/threats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getThreatSummary() {
        Map<String, Object> summary = threatAlertService.getThreatSummary();
        return ResponseEntity.ok(ApiResponse.ok("Threat summary fetched", summary));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<ThreatAlert>>> getAlerts(
            @RequestParam(defaultValue = "all") String filter) {

        List<ThreatAlert> alerts;
        if ("active".equals(filter)) {
            alerts = threatAlertService.getActiveAlerts();
        } else if ("critical".equals(filter)) {
            alerts = threatAlertService.getAlertsBySeverity("CRITICAL");
        } else if ("high".equals(filter)) {
            alerts = threatAlertService.getAlertsBySeverity("HIGH");
        } else {
            alerts = threatAlertService.getAllAlerts();
        }

        return ResponseEntity.ok(ApiResponse.ok("Alerts fetched", alerts));
    }

    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<ApiResponse<ThreatAlert>> resolveAlert(@PathVariable Long id) {
        return threatAlertService.resolveAlert(id)
            .map(alert -> ResponseEntity.ok(ApiResponse.ok("Alert resolved", alert)))
            .orElse(ResponseEntity.notFound().<ApiResponse<ThreatAlert>>build());
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Object>> getLogs(
            @RequestParam(defaultValue = "all") String type) {

        var logs = "all".equals(type) ? logService.getRecentLogs() : logService.getLogsByType(type.toUpperCase());
        return ResponseEntity.ok(ApiResponse.ok("Logs fetched", logs));
    }
}
