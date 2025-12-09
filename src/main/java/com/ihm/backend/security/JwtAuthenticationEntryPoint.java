// src/main/java/com/ihm/backend/security/JwtAuthenticationEntryPoint.java

package com.ihm.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.domain.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> apiResponse = ApiResponse.unauthorized(
                "Accès refusé",
                "Vous devez être connecté pour accéder à cette ressource"
        );

        String json = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(json);
    }
}