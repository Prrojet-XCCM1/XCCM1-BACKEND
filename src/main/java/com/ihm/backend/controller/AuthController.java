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
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/register")
    @Deprecated // Garder pour compatibilité
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Endpoint d'inscription pour les étudiants
     */
    @PostMapping("/register/student")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> registerStudent(
            @RequestBody StudentRegisterRequest request) {
        ApiResponse<AuthenticationResponse> response = authService.registerStudent(request);
        
        if (response.getStatus() == 201) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Endpoint d'inscription pour les enseignants
     */
    @PostMapping("/register/teacher")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> registerTeacher(
            @RequestBody TeacherRegisterRequest request) {
        ApiResponse<AuthenticationResponse> response = authService.registerTeacher(request);
        
        if (response.getStatus() == 201) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(
            @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(authService.requestPasswordReset(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(
            @RequestBody PasswordUpdateRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}