package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.*;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.security.oauth2.CustomOAuth2UserService;
import com.ihm.backend.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.ihm.backend.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.ihm.backend.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

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

        @Test
        @DisplayName("POST /api/v1/auth/forgot-password - Email invalide → 400")
        void forgotPassword_invalidEmail_returns400() throws Exception {
            PasswordResetRequest req = new PasswordResetRequest("not-an-email");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/v1/auth/forgot-password - Email vide → 400")
        void forgotPassword_blankEmail_returns400() throws Exception {
            PasswordResetRequest req = new PasswordResetRequest("");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Réinitialisation de mot de passe")
    class ResetPassword {

        @Test
        @DisplayName("POST /api/v1/auth/reset-password - Succès")
        void resetPassword_success() throws Exception {
            PasswordUpdateRequest req = new PasswordUpdateRequest("valid-token", "NewPass123", "NewPass123");
            when(authService.resetPassword(any())).thenReturn(ApiResponse.success("Mot de passe réinitialisé avec succès"));

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("POST /api/v1/auth/reset-password - Token expiré → 400")
        void resetPassword_expiredToken_returns400() throws Exception {
            PasswordUpdateRequest req = new PasswordUpdateRequest("expired-token", "NewPass123", "NewPass123");
            when(authService.resetPassword(any())).thenReturn(ApiResponse.badRequest("Token expiré", null));

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("POST /api/v1/auth/reset-password - Mot de passe trop court → 400")
        void resetPassword_shortPassword_returns400() throws Exception {
            PasswordUpdateRequest req = new PasswordUpdateRequest("valid-token", "short", "short");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}
