package com.loganalytics.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/dashboard")
    public String userDashboard() {
        return "user-dashboard";
    }

    @GetMapping("/admin")
    public String adminDashboard() {
        return "admin-dashboard";
    }
}
