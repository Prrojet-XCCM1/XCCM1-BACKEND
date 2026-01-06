package com.ihm.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")   // URL path
                .addResourceLocations("file:uploads/"); // folder path
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Autoriser TOUTES les origines
        registry.addMapping("/**")  // Appliquer à tous les endpoints
                .allowedOrigins("*")  // Permettre toutes les origines
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600); // Cache pendant 1 heure
        
        // OU version plus restrictive mais toujours ouverte :
        /*
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // Utiliser allowedOriginPatterns pour plus de flexibilité
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(false) // Doit être false quand allowedOrigins("*")
                .maxAge(3600);
        */
    }
}