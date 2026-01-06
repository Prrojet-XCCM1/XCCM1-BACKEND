package com.ihm.backend.controller;


import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.StudentResponse;
import com.ihm.backend.dto.response.TeacherResponse;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Gestion des Utilisateurs", description = "API pour la gestion des utilisateurs par rôle")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Récupérer un étudiant par ID", 
               description = "Retourne les détails d'un étudiant spécifique")
    @GetMapping("/students/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentById(
            @Parameter(description = "ID de l'étudiant") @PathVariable UUID id) {
        StudentResponse student = userService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.success("Étudiant récupéré avec succès", student));
    }

    @Operation(summary = "Récupérer un enseignant par ID", 
               description = "Retourne les détails d'un enseignant spécifique")
    @GetMapping("/teachers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<TeacherResponse>> getTeacherById(
            @Parameter(description = "ID de l'enseignant") @PathVariable UUID id) {
        TeacherResponse teacher = userService.getTeacherById(id);
        return ResponseEntity.ok(ApiResponse.success("Enseignant récupéré avec succès", teacher));
    }

    @Operation(summary = "Lister tous les étudiants", 
               description = "Retourne une page d'étudiants avec pagination")
    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> getAllStudents(
            @Parameter(description = "Paramètres de pagination") @PageableDefault(size = 20) Pageable pageable) {
        Page<StudentResponse> students = userService.getAllStudents(pageable);
        return ResponseEntity.ok(ApiResponse.success("Liste des étudiants récupérée", students));
    }

    @Operation(summary = "Lister tous les enseignants", 
               description = "Retourne une page d'enseignants avec pagination")
    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<Page<TeacherResponse>>> getAllTeachers(
            @Parameter(description = "Paramètres de pagination") @PageableDefault(size = 20) Pageable pageable) {
        Page<TeacherResponse> teachers = userService.getAllTeachers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Liste des enseignants récupérée", teachers));
    }

    @Operation(summary = "Récupérer l'utilisateur connecté", 
               description = "Retourne les informations de l'utilisateur actuellement connecté")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getCurrentUser() {
        var currentUser = userService.getCurrentUser();
        
        // Retourne la réponse appropriée selon le rôle
        if (currentUser.getRole() == UserRole.STUDENT) {
            StudentResponse student = mapToStudentResponse(currentUser);
            return ResponseEntity.ok(ApiResponse.success("Profil étudiant récupéré", student));
        } else if (currentUser.getRole() == UserRole.TEACHER) {
            TeacherResponse teacher = mapToTeacherResponse(currentUser);
            return ResponseEntity.ok(ApiResponse.success("Profil enseignant récupéré", teacher));
        } else {
            // Pour les admins ou autres rôles, retourner l'entité complète ou un DTO AdminResponse
            return ResponseEntity.ok(ApiResponse.success("Profil utilisateur récupéré", currentUser));
        }
    }

    // Méthodes de mapping (similaires à celles du service)
    private StudentResponse mapToStudentResponse(com.ihm.backend.entity.User user) {
        return StudentResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .photoUrl(user.getPhotoUrl())
                .city(user.getCity())
                .university(user.getUniversity())
                .specialization(user.getSpecialization())
                .registrationDate(user.getRegistrationDate())
                .active(user.isActive())
                .verified(user.isVerified())
                .build();
    }

    private TeacherResponse mapToTeacherResponse(com.ihm.backend.entity.User user) {
        List<String> subjects = null;
        if (user.getSubjects() != null && !user.getSubjects().isEmpty()) {
            subjects = Arrays.asList(user.getSubjects().split(","));
        }
        
        return TeacherResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .photoUrl(user.getPhotoUrl())
                .city(user.getCity())
                .university(user.getUniversity())
                .grade(user.getGrade())
                .certification(user.getCertification())
                .subjects(subjects)
                .registrationDate(user.getRegistrationDate())
                .active(user.isActive())
                .verified(user.isVerified())
                .build();
    }
}