package com.loganalytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogAnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogAnalyticsApplication.class, args);
    }
}
