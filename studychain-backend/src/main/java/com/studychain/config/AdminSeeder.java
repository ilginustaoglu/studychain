package com.studychain.config;

import com.studychain.models.User;
import com.studychain.models.UserRole;
import com.studychain.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    public AdminSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }
        // Ensure the specified admin user exists and is ADMIN
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
            u -> {
                if (u.getRole() != UserRole.ADMIN) {
                    u.setRole(UserRole.ADMIN);
                    userRepository.save(u);
                }
            },
            () -> {
                User user = new User();
                user.setEmail(adminEmail);
                user.setUsername(adminEmail);
                user.setFirstName("Admin");
                user.setLastName("User");
                user.setPassword((adminPassword != null && !adminPassword.isBlank()) ? adminPassword : "change-me-now");
                user.setRole(UserRole.ADMIN);
                userRepository.save(user);
                System.out.println("[StudyChain] Admin user ensured: " + adminEmail);
            }
        );

        // Demote every other user to USER (normal)
        userRepository.findAll().forEach(u -> {
            if (!adminEmail.equalsIgnoreCase(u.getEmail()) && u.getRole() != UserRole.USER) {
                u.setRole(UserRole.USER);
                userRepository.save(u);
            }
        });
    }
}


