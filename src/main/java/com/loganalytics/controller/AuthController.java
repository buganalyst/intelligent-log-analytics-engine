package com.loganalytics.controller;

import com.loganalytics.dto.ApiResponse;
import com.loganalytics.dto.LoginRequest;
import com.loganalytics.dto.RegisterRequest;
import com.loganalytics.engine.ThreatDetectionEngine;
import com.loganalytics.model.AppUser;
import com.loganalytics.service.LogService;
import com.loganalytics.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/svc/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private LogService logService;

    @Autowired
    private ThreatDetectionEngine threatDetectionEngine;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpSession session) {

        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        Optional<AppUser> userOpt = userService.authenticate(request.getUsername(), request.getPassword());

        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());

            var logEntry = logService.createLog(
                "AUTH", ip, request.getUsername(), "LOGIN_SUCCESS",
                "User logged in successfully from " + ip,
                "SUCCESS", userAgent, "/svc/auth/login", 200, null
            );
            threatDetectionEngine.analyzeLog(logEntry);

            return ResponseEntity.ok(ApiResponse.ok("Login successful", new java.util.HashMap<String, Object>() {{
                put("username", user.getUsername());
                put("role", user.getRole());
                put("email", user.getEmail());
            }}));
        } else {
            var logEntry = logService.createLog(
                "AUTH", ip, request.getUsername(), "LOGIN_FAILED",
                "Failed login attempt for user: " + request.getUsername() + " from IP: " + ip,
                "FAILED", userAgent, "/svc/auth/login", 401, null
            );
            threatDetectionEngine.analyzeLog(logEntry);

            return ResponseEntity.status(401).body(ApiResponse.error("Invalid username or password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(
            @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        if (userService.usernameExists(request.getUsername())) {
            var logEntry = logService.createLog(
                "AUTH", ip, request.getUsername(), "REGISTER_FAILED",
                "Registration attempt with existing username: " + request.getUsername(),
                "FAILED", userAgent, "/svc/auth/register", 409, null
            );
            threatDetectionEngine.analyzeLog(logEntry);
            return ResponseEntity.status(409).body(ApiResponse.error("Username already exists"));
        }

        if (userService.emailExists(request.getEmail())) {
            return ResponseEntity.status(409).body(ApiResponse.error("Email already in use"));
        }

        AppUser newUser = userService.registerUser(request.getUsername(), request.getPassword(), request.getEmail());

        var logEntry = logService.createLog(
            "AUTH", ip, request.getUsername(), "REGISTER_SUCCESS",
            "New user registered: " + request.getUsername() + " (" + request.getEmail() + ")",
            "SUCCESS", userAgent, "/svc/auth/register", 201, null
        );
        threatDetectionEngine.analyzeLog(logEntry);

        return ResponseEntity.status(201).body(ApiResponse.ok("Registration successful", new java.util.HashMap<String, String>() {{
            put("username", newUser.getUsername());
            put("email", newUser.getEmail());
        }}));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session, HttpServletRequest httpRequest) {
        String username = (String) session.getAttribute("username");
        String ip = getClientIp(httpRequest);

        if (username != null) {
            var logEntry = logService.createLog(
                "AUTH", ip, username, "LOGOUT",
                "User logged out: " + username,
                "SUCCESS", httpRequest.getHeader("User-Agent"), "/svc/auth/logout", 200, null
            );
            threatDetectionEngine.analyzeLog(logEntry);
        }
        session.invalidate();
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<Object>> getSession(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            return ResponseEntity.ok(ApiResponse.ok("Session active", new java.util.HashMap<String, String>() {{
                put("username", username);
                put("role", (String) session.getAttribute("role"));
            }}));
        }
        return ResponseEntity.ok(ApiResponse.ok("No active session", null));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
