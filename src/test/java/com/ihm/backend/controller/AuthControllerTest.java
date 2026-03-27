package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.*;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Authentification")
    class Authentication {
        @Test
        @DisplayName("POST /api/v1/auth/login - Succès")
        void login_success() throws Exception {
            AuthenticationRequest req = new AuthenticationRequest("test@test.com", "password");
            AuthenticationResponse res = AuthenticationResponse.builder().token("jwt-token").build();
            when(authService.authenticate(any())).thenReturn(ApiResponse.success("Succès", res));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.token").value("jwt-token"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/login - Identifiants invalides (401)")
        void login_unauthorized() throws Exception {
            AuthenticationRequest req = new AuthenticationRequest("wrong@test.com", "wrong");
            // Simuler un comportement d'échec retourné par le service (ou via une exception)
            when(authService.authenticate(any())).thenReturn(ApiResponse.unauthorized("Identifiants invalides", "Bad credentials"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Inscription")
    class Registration {
        @Test
        @DisplayName("POST /api/v1/auth/register/student - Succès")
        void registerStudent_success() throws Exception {
            StudentRegisterRequest req = new StudentRegisterRequest();
            req.setEmail("new@student.com");
            req.setPassword("password123");
            req.setFirstName("Student");
            req.setLastName("Test");
            
            AuthenticationResponse res = AuthenticationResponse.builder().token("jwt-token").build();
            when(authService.registerStudent(any())).thenReturn(ApiResponse.created("Succès", res));

            mockMvc.perform(post("/api/v1/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.token").value("jwt-token"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/register/student - Email déjà utilisé (400)")
        void registerStudent_conflict() throws Exception {
            StudentRegisterRequest req = new StudentRegisterRequest();
            req.setEmail("exists@test.com");

            when(authService.registerStudent(any())).thenReturn(ApiResponse.badRequest("Email déjà utilisé", null));

            mockMvc.perform(post("/api/v1/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Mot de passe oublié")
    class PasswordReset {
        @Test
        @DisplayName("POST /api/v1/auth/forgot-password - Succès")
        void forgotPassword_success() throws Exception {
            PasswordResetRequest req = new PasswordResetRequest("test@test.com");
            when(authService.requestPasswordReset(any())).thenReturn(ApiResponse.success("Email envoyé"));

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email envoyé"));
        }
    }
}
