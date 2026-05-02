package com.ihm.backend.controller;

import com.ihm.backend.dto.request.*;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;
import com.ihm.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin // ou géré par le CorsConfigurationSource ci-dessus
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @RequestBody AuthenticationRequest request) {
        ApiResponse<AuthenticationResponse> response = authService.authenticate(request);
        int code = response.getCode() != 0 ? response.getCode() : 200;
        return ResponseEntity.status(code).body(response);
    }

    @PostMapping("/register")
    @Deprecated // Garder pour compatibilité
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(
            @RequestBody @jakarta.validation.Valid RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Endpoint d'inscription pour les étudiants
     */
    @PostMapping("/register/student")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> registerStudent(
            @RequestBody @jakarta.validation.Valid StudentRegisterRequest request) {
        ApiResponse<AuthenticationResponse> response = authService.registerStudent(request);

        if (response.getCode() == 201) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.status(response.getCode()).body(response);
    }

    /**
     * Endpoint d'inscription pour les enseignants
     */
    @PostMapping("/register/teacher")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> registerTeacher(
            @RequestBody @jakarta.validation.Valid TeacherRegisterRequest request) {
        ApiResponse<AuthenticationResponse> response = authService.registerTeacher(request);

        if (response.getCode() == 201) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(
            @RequestBody @jakarta.validation.Valid PasswordResetRequest request) {
        ApiResponse<?> response = authService.requestPasswordReset(request);
        int status = response.getCode() != 0 ? response.getCode() : 200;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(
            @RequestBody @jakarta.validation.Valid PasswordUpdateRequest request) {
        ApiResponse<?> response = authService.resetPassword(request);
        int status = response.getCode() != 0 ? response.getCode() : 200;
        return ResponseEntity.status(status).body(response);
    }
}