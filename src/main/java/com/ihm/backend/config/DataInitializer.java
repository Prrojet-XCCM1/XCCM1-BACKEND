package com.ihm.backend.config;

import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.countByRole(UserRole.ADMIN) == 0) {
            log.info("Aucun administrateur trouvé en base. Création du compte administrateur par défaut...");

            String adminEmail = System.getenv().getOrDefault("ADMIN_EMAIL", "admin@xccm.com");
            String adminPassword = System.getenv("ADMIN_PASSWORD");
            if (adminPassword == null || adminPassword.isBlank()) {
                adminPassword = java.util.UUID.randomUUID().toString();
                log.warn("Variable ADMIN_PASSWORD non définie — mot de passe aléatoire généré. Définissez ADMIN_PASSWORD et relancez l'application.");
            }

            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName("Système")
                    .lastName("Admin")
                    .role(UserRole.ADMIN)
                    .registrationDate(LocalDateTime.now())
                    .active(true)
                    .verified(true)
                    .build();

            userRepository.save(admin);
            log.info("Compte administrateur créé : {}", adminEmail);
        }
    }
}
