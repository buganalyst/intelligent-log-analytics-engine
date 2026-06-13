package com.loganalytics.config;

import com.loganalytics.model.AppUser;
import com.loganalytics.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AppUserRepository userRepository;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new AppUser("admin", "admin123", "admin@loganalytics.com", "ADMIN"));
        }
        if (!userRepository.existsByUsername("alice")) {
            userRepository.save(new AppUser("alice", "password123", "alice@example.com", "USER"));
        }
        if (!userRepository.existsByUsername("bob")) {
            userRepository.save(new AppUser("bob", "pass456", "bob@example.com", "USER"));
        }
        if (!userRepository.existsByUsername("charlie")) {
            userRepository.save(new AppUser("charlie", "charlie789", "charlie@example.com", "USER"));
        }
    }
}
