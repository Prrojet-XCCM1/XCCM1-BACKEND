package com.ihm.backend.controller;

import com.ihm.backend.dto.request.*;
import com.ihm.backend.dto.response.*;
import com.ihm.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "API pour les opérations d'administration")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ==================== Statistiques ====================

    @Operation(summary = "Récupérer les statistiques globales")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatisticsResponse>> getStatistics() {
        return ResponseEntity.ok(adminService.getStatistics());
    }

    @Operation(summary = "Récupérer les statistiques détaillées des enrollments")
    @GetMapping("/enrollments/stats")
    public ResponseEntity<ApiResponse<EnrollmentStatsResponse>> getEnrollmentStatistics() {
        return ResponseEntity.ok(adminService.getEnrollmentStatistics());
    }

    // ==================== Création d'utilisateurs ====================

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
            @RequestBody @Valid RegisterRequest request) {
        ApiResponse<AuthenticationResponse> response = adminService.createAdmin(request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    // ==================== CRUD Users (tous types) ====================

    @Operation(summary = "Lister tous les utilisateurs")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDetailResponse>>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @Operation(summary = "Récupérer un utilisateur par ID")
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @Operation(summary = "Mettre à jour un utilisateur")
    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
            @PathVariable UUID id,
            @RequestBody @Valid UserUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    @Operation(summary = "Supprimer un utilisateur")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.deleteUser(id));
    }

    // ==================== CRUD Students ====================

    @Operation(summary = "Lister tous les étudiants")
    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<UserDetailResponse>>> getAllStudents() {
        return ResponseEntity.ok(adminService.getAllStudents());
    }

    @Operation(summary = "Récupérer un étudiant par ID")
    @GetMapping("/students/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getStudentById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getStudentById(id));
    }

    @Operation(summary = "Mettre à jour un étudiant")
    @PutMapping("/students/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateStudent(
            @PathVariable UUID id,
            @RequestBody @Valid UserUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateStudent(id, request));
    }

    @Operation(summary = "Supprimer un étudiant")
    @DeleteMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.deleteStudent(id));
    }

    // ==================== CRUD Teachers ====================

    @Operation(summary = "Lister tous les enseignants")
    @GetMapping("/teachers")
    public ResponseEntity<ApiResponse<List<UserDetailResponse>>> getAllTeachers() {
        return ResponseEntity.ok(adminService.getAllTeachers());
    }

    @Operation(summary = "Récupérer un enseignant par ID")
    @GetMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getTeacherById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getTeacherById(id));
    }

    @Operation(summary = "Mettre à jour un enseignant")
    @PutMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateTeacher(
            @PathVariable UUID id,
            @RequestBody @Valid UserUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateTeacher(id, request));
    }

    @Operation(summary = "Supprimer un enseignant")
    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.deleteTeacher(id));
    }

    // ==================== CRUD Courses ====================

    @Operation(summary = "Lister tous les cours")
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllCourses() {
        return ResponseEntity.ok(adminService.getAllCourses());
    }

    @Operation(summary = "Récupérer un cours par ID")
    @GetMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(@PathVariable Integer id) {
        return ResponseEntity.ok(adminService.getCourseById(id));
    }

    @Operation(summary = "Mettre à jour un cours")
    @PutMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable Integer id,
            @RequestBody @Valid CourseUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateCourse(id, request));
    }

    @Operation(summary = "Supprimer un cours")
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Integer id) {
        return ResponseEntity.ok(adminService.deleteCourse(id));
    }

    // ==================== CRUD Enrollments ====================

    @Operation(summary = "Lister tous les enrollments")
    @GetMapping("/enrollments")
    public ResponseEntity<ApiResponse<List<EnrollmentDetailResponse>>> getAllEnrollments() {
        return ResponseEntity.ok(adminService.getAllEnrollments());
    }

    @Operation(summary = "Récupérer un enrollment par ID")
    @GetMapping("/enrollments/{id}")
    public ResponseEntity<ApiResponse<EnrollmentDetailResponse>> getEnrollmentById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getEnrollmentById(id));
    }

    @Operation(summary = "Mettre à jour un enrollment")
    @PutMapping("/enrollments/{id}")
    public ResponseEntity<ApiResponse<EnrollmentDetailResponse>> updateEnrollment(
            @PathVariable Long id,
            @RequestBody @Valid EnrollmentUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateEnrollment(id, request));
    }

    @Operation(summary = "Supprimer un enrollment")
    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteEnrollment(id));
    }
}
