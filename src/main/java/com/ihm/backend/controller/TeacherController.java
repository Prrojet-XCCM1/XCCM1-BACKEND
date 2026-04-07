package com.ihm.backend.controller;

import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.TeacherCourseStatsResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.service.TeacherStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
@Tag(name = "Enseignant", description = "API pour les statistiques et fonctionnalités enseignant")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final TeacherStatsService teacherStatsService;
    private final com.ihm.backend.service.ExerciseService exerciseService;

    @Operation(summary = "Récupérer les statistiques d'un cours spécifique")
    @GetMapping("/courses/{courseId}/stats")
    public ResponseEntity<ApiResponse<TeacherCourseStatsResponse>> getCourseStatistics(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(teacherStatsService.getCourseStatistics(teacher.getId(), courseId));
    }

    @Operation(summary = "Récupérer les statistiques de tous les cours de l'enseignant")
    @GetMapping("/courses/stats")
    public ResponseEntity<ApiResponse<List<TeacherCourseStatsResponse>>> getAllCoursesStatistics(
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(teacherStatsService.getAllCoursesStatistics(teacher.getId()));
    }

    // --- Gestion des exercices ---

    @Operation(summary = "Créer un exercice pour un cours")
    @PostMapping("/courses/{courseId}/exercises")
    public ResponseEntity<ApiResponse<com.ihm.backend.dto.response.ExerciseResponse>> createExercise(
            @PathVariable Integer courseId,
            @RequestBody com.ihm.backend.dto.request.ExerciseCreateRequest request,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(exerciseService.createExercise(courseId, teacher.getId(), request));
    }

    @Operation(summary = "Mettre à jour un exercice")
    @PutMapping("/exercises/{exerciseId}")
    public ResponseEntity<ApiResponse<com.ihm.backend.dto.response.ExerciseResponse>> updateExercise(
            @PathVariable Integer exerciseId,
            @RequestBody com.ihm.backend.dto.request.ExerciseUpdateRequest request,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(exerciseService.updateExercise(exerciseId, teacher.getId(), request));
    }

    @Operation(summary = "Supprimer un exercice")
    @DeleteMapping("/exercises/{exerciseId}")
    public ResponseEntity<ApiResponse<Void>> deleteExercise(
            @PathVariable Integer exerciseId,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(exerciseService.deleteExercise(exerciseId, teacher.getId()));
    }

    @Operation(summary = "Lister les soumissions pour un exercice")
    @GetMapping("/exercises/{exerciseId}/submissions")
    public ResponseEntity<ApiResponse<List<com.ihm.backend.dto.response.StudentExerciseResponse>>> getSubmissions(
            @PathVariable Integer exerciseId,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(exerciseService.getSubmissionsForExercise(exerciseId, teacher.getId()));
    }

    @Operation(summary = "Noter une soumission")
    @PutMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<ApiResponse<com.ihm.backend.dto.response.StudentExerciseResponse>> gradeSubmission(
            @PathVariable Long submissionId,
            @RequestBody com.ihm.backend.dto.request.GradeSubmissionRequest request,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(exerciseService.gradeSubmission(submissionId, teacher.getId(), request));
    }

    @Operation(summary = "Supprimer une soumission")
    @DeleteMapping("/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(exerciseService.deleteSubmission(submissionId, teacher.getId()));
    }
}
