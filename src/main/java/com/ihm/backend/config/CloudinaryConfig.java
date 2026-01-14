package com.ihm.backend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Cloudinary integration.
 * Initializes Cloudinary client with credentials from environment variables.
 */
@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String apiKey = System.getenv("CLOUDINARY_API_KEY");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET");

        if (cloudName == null || apiKey == null || apiSecret == null) {
            throw new IllegalStateException(
                "Cloudinary credentials are not configured. Please set CLOUDINARY_CLOUD_NAME, " +
                "CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET environment variables."
            );
        }

        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
    }
}
