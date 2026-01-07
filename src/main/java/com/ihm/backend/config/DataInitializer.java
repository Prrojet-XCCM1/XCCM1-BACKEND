package com.ihm.backend.config;

import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.repository.UserRepository;
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
        if (userRepository.count() == 0) {
            log.info("Aucun utilisateur trouvé en base. Création du compte administrateur par défaut...");

            User admin = User.builder()
                    .email("admin@xccm.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Système")
                    .lastName("Admin")
                    .role(UserRole.ADMIN)
                    .registrationDate(LocalDateTime.now())
                    .active(true)
                    .verified(true)
                    .build();

            userRepository.save(admin);
            log.info("Compte administrateur par défaut créé : admin@xccm.com / admin123");
        }
    }
}
