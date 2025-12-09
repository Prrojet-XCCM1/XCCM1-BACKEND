package com.ihm.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration

@RequiredArgsConstructor
public class ReactiveApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public ReactiveUserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .cast(org.springframework.security.core.userdetails.UserDetails.class)
                .switchIfEmpty(Mono.error(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found")));
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager(
            ReactiveUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        UserDetailsRepositoryReactiveAuthenticationManager authManager = 
            new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        authManager.setPasswordEncoder(passwordEncoder);
        return authManager;
    }
}