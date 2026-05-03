package com.ihm.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Dédié au bean PasswordEncoder pour éviter une dépendance circulaire avec
 * {@link SecurityConfig} (qui injecte CustomOAuth2UserService, lui-même dépendant du PasswordEncoder).
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
