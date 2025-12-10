// src/main/java/com/ihm/backend/config/JacksonConfig.java

package com.ihm.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    // ISO 8601 avec millisecondes

    @Bean
    @Primary // LE PLUS IMPORTANT : Spring utilisera TOUJOURS cet ObjectMapper
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();
        
        // On force le bon format pour LocalDateTime partout
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(java.time.LocalDateTime.class,
            new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer(
                DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        module.addDeserializer(java.time.LocalDateTime.class,
            new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer(
                DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        
        mapper.registerModule(module);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }

    // Ce bean est utilisé par Spring Boot pour construire l'ObjectMapper principal
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .timeZone("Africa/Douala") // ou "Europe/Paris" selon ton fuseau
                .simpleDateFormat(DATE_TIME_PATTERN);
    }
}