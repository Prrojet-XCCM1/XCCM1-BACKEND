package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.*;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;
import com.ihm.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Login success")
    void login_success() throws Exception {
        AuthenticationRequest req = new AuthenticationRequest("test@test.com", "password");
        AuthenticationResponse res = AuthenticationResponse.builder().token("jwt").build();
        when(authService.authenticate(any())).thenReturn(ApiResponse.success("success", res));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register/student - Register student success")
    void registerStudent_success() throws Exception {
        StudentRegisterRequest req = new StudentRegisterRequest();
        req.setEmail("new@student.com");
        AuthenticationResponse res = AuthenticationResponse.builder().token("jwt").build();
        when(authService.registerStudent(any())).thenReturn(ApiResponse.created("success", res));

        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").value("jwt"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Forgot password success")
    void forgotPassword_success() throws Exception {
        PasswordResetRequest req = new PasswordResetRequest("test@test.com");
        when(authService.requestPasswordReset(any())).thenReturn(ApiResponse.success("Email sent"));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email sent"));
    }
}
