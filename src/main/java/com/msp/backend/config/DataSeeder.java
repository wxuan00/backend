package com.msp.backend.config;

import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Only create if no users exist
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setEmail("admin@msp.com");
            admin.setPassword(passwordEncoder.encode("password123")); // Hash the password
            admin.setRole("ADMIN");
            admin.setPhoneNumber("00000000");
            admin.setStatus("ACTIVE");
            admin.setMfaEnabled(false);

            userRepository.save(admin);
            System.out.println("SUPER ADMIN CREATED: admin@msp.com / password123");
        }
    }
}