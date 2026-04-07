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
        // Autoriser des origines spécifiques pour plus de sécurité (compatible avec les credentials)
        String[] allowedOrigins = {
            "http://localhost:3000", 
            "http://192.168.1.177:3000", 
            "http://192.168.1.135:3000",
            "https://frontend-xccm-12027.vercel.app"
        };
        
        registry.addMapping("/**")  // Appliquer à tous les endpoints
                .allowedOrigins(allowedOrigins) // Utiliser allowedOrigins explicites
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Cache pendant 1 heure
    }
}