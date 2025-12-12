package com.chess.config;

import com.chess.model.User;
import com.chess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes test users on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            createTestUser("alice@example.com", "Password1!", "Alice");
            createTestUser("bob@example.com", "Password1!", "Bob");
            log.info("Test users created successfully");
        } else {
            log.info("Users already exist, skipping initialization");
        }
    }

    private void createTestUser(String email, String password, String displayName) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName)
                .online(false)
                .build();
        userRepository.save(user);
        log.info("Created test user: {}", email);
    }
}



