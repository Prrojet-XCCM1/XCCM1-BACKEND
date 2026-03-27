package com.ihm.backend.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Error parsing JSON string: {}. Returning empty map.", json, e);
            // Handle cases like "bonjour" which are not valid JSON
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("raw_content", json);
            return fallback;
        }
    }

    public static String toJson(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Error converting map to JSON string", e);
            return null;
        }
    }
}
