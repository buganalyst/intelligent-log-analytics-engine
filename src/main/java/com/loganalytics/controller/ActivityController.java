package com.loganalytics.controller;

import com.loganalytics.dto.ApiResponse;
import com.loganalytics.engine.ThreatDetectionEngine;
import com.loganalytics.model.LogEntry;
import com.loganalytics.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/svc/activity")
public class ActivityController {

    @Autowired
    private LogService logService;

    @Autowired
    private ThreatDetectionEngine threatDetectionEngine;

    private final Random random = new Random();

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Object>> search(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpSession session) {

        String query = body.getOrDefault("query", "");
        String username = getSessionUser(session);
        String ip = getClientIp(request);
        long startTime = System.currentTimeMillis();

        LogEntry logEntry = logService.createLog(
            "ACTIVITY", ip, username, "SEARCH",
            "Search query: " + query,
            "SUCCESS", request.getHeader("User-Agent"),
            "/svc/activity/search", 200,
            System.currentTimeMillis() - startTime
        );
        threatDetectionEngine.analyzeLog(logEntry);

        Map<String, Object> results = new HashMap<>();
        results.put("query", query);
        results.put("resultCount", random.nextInt(50));
        results.put("timeTaken", (System.currentTimeMillis() - startTime) + "ms");

        return ResponseEntity.ok(ApiResponse.ok("Search completed", results));
    }

    @PostMapping("/submit-form")
    public ResponseEntity<ApiResponse<Object>> submitForm(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpSession session) {

        String formData = body.toString();
        String username = getSessionUser(session);
        String ip = getClientIp(request);

        LogEntry logEntry = logService.createLog(
            "ACTIVITY", ip, username, "FORM_SUBMIT",
            "Form submitted with data: " + formData.substring(0, Math.min(formData.length(), 200)),
            "SUCCESS", request.getHeader("User-Agent"),
            "/svc/activity/submit-form", 200, null
        );
        threatDetectionEngine.analyzeLog(logEntry);

        return ResponseEntity.ok(ApiResponse.ok("Form submitted successfully", body));
    }

    @GetMapping("/page-visit")
    public ResponseEntity<ApiResponse<Void>> pageVisit(
            @RequestParam String page,
            HttpServletRequest request,
            HttpSession session) {

        String username = getSessionUser(session);
        String ip = getClientIp(request);

        LogEntry logEntry = logService.createLog(
            "ACTIVITY", ip, username, "PAGE_VISIT",
            "User visited page: " + page,
            "SUCCESS", request.getHeader("User-Agent"),
            page, 200, (long) (random.nextInt(500) + 50)
        );
        threatDetectionEngine.analyzeLog(logEntry);

        return ResponseEntity.ok(ApiResponse.ok("Visit logged", null));
    }

    @PostMapping("/download")
    public ResponseEntity<ApiResponse<Object>> downloadFile(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpSession session) {

        String filename = body.getOrDefault("filename", "unknown");
        String username = getSessionUser(session);
        String ip = getClientIp(request);

        LogEntry logEntry = logService.createLog(
            "ACTIVITY", ip, username, "FILE_DOWNLOAD",
            "File download requested: " + filename,
            "SUCCESS", request.getHeader("User-Agent"),
            "/svc/activity/download", 200, null
        );
        threatDetectionEngine.analyzeLog(logEntry);

        Map<String, String> result = new HashMap<>();
        result.put("filename", filename);
        result.put("status", "Download initiated");

        return ResponseEntity.ok(ApiResponse.ok("Download started", result));
    }

    @PostMapping("/simulate-attack")
    public ResponseEntity<ApiResponse<Object>> simulateAttack(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpSession session) {

        String attackType = body.getOrDefault("attackType", "BRUTE_FORCE");
        String targetUser = body.getOrDefault("targetUser", "admin");
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        switch (attackType) {
            case "BRUTE_FORCE":
                for (int i = 0; i < 7; i++) {
                    LogEntry entry = logService.createLog(
                        "AUTH", ip, targetUser, "LOGIN_FAILED",
                        "Simulated brute force attempt #" + (i + 1) + " on user: " + targetUser,
                        "FAILED", userAgent, "/api/auth/login", 401, null
                    );
                    threatDetectionEngine.analyzeLog(entry);
                }
                break;

            case "SQL_INJECTION":
                String sqlPayload = "' OR 1=1; DROP TABLE users; --";
                LogEntry sqlEntry = logService.createLog(
                    "ACTIVITY", ip, getSessionUser(session), "SEARCH",
                    "SQL_INJECTION attempt detected. Query: " + sqlPayload,
                    "BLOCKED", userAgent, "/svc/activity/search?q=" + sqlPayload, 400, null
                );
                threatDetectionEngine.analyzeLog(sqlEntry);
                break;

            case "XSS":
                String xssPayload = "<script>alert('XSS')</script>";
                LogEntry xssEntry = logService.createLog(
                    "ACTIVITY", ip, getSessionUser(session), "FORM_SUBMIT",
                    "XSS payload detected in form: " + xssPayload,
                    "BLOCKED", userAgent, "/svc/activity/submit-form", 400, null
                );
                threatDetectionEngine.analyzeLog(xssEntry);
                break;

            case "PATH_TRAVERSAL":
                String traversalPath = "../../etc/passwd";
                LogEntry traversalEntry = logService.createLog(
                    "ACTIVITY", ip, getSessionUser(session), "FILE_ACCESS",
                    "Path traversal attempt: " + traversalPath,
                    "BLOCKED", userAgent, "/files/" + traversalPath, 403, null
                );
                threatDetectionEngine.analyzeLog(traversalEntry);
                break;
        }

        return ResponseEntity.ok(ApiResponse.ok("Attack simulation completed. Check dashboard for alerts.", null));
    }

    private String getSessionUser(HttpSession session) {
        String user = (String) session.getAttribute("username");
        return user != null ? user : "ANONYMOUS";
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
