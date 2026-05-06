package com.ihm.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Web MVC.
 *
 * <p>La configuration CORS est gérée exclusivement dans {@link SecurityConfig#corsConfigurationSource()}
 * via Spring Security, qui a priorité sur WebMvcConfigurer.addCorsMappings().
 * Ne pas dupliquer la config CORS ici.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir les fichiers uploadés (images de couverture, etc.) depuis le dossier local
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}