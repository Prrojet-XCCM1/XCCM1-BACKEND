package com.ihm.backend.controller;

import com.ihm.backend.dto.request.RegisterRequest;
import com.ihm.backend.dto.request.StudentRegisterRequest;
import com.ihm.backend.dto.request.TeacherRegisterRequest;
import com.ihm.backend.dto.response.AdminStatisticsResponse;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;
import com.ihm.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "API pour les opérations d'administration")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Récupérer les statistiques globales")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatisticsResponse>> getStatistics() {
        return ResponseEntity.ok(adminService.getStatistics());
    }

    @Operation(summary = "Créer un étudiant (par l'admin)")
    @PostMapping("/users/student")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> createStudent(
            @RequestBody @Valid StudentRegisterRequest request) {
        ApiResponse<AuthenticationResponse> response = adminService.createStudent(request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @Operation(summary = "Créer un enseignant (par l'admin)")
    @PostMapping("/users/teacher")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> createTeacher(
            @RequestBody @Valid TeacherRegisterRequest request) {
        ApiResponse<AuthenticationResponse> response = adminService.createTeacher(request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @Operation(summary = "Créer un administrateur (par l'admin)")
    @PostMapping("/users/admin")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> createAdmin(
            @RequestBody @Valid com.ihm.backend.dto.request.RegisterRequest request) {
        ApiResponse<AuthenticationResponse> response = adminService.createAdmin(request);
        return ResponseEntity.status(response.getCode()).body(response);
    }
}
