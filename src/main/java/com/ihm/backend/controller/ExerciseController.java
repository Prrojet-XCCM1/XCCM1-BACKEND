package com.ihm.backend.controller;

import com.ihm.backend.dto.request.StudentExerciseSubmissionRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.ExerciseResponse;
import com.ihm.backend.dto.response.StudentExerciseResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.service.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercices", description = "API pour la gestion des exercices (côté étudiant)")
@PreAuthorize("isAuthenticated()")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @Operation(summary = "Lister les exercices d'un cours")
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<ExerciseResponse>>> getExercisesForCourse(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(exerciseService.getExercisesForCourse(courseId, user.getId()));
    }

    @Operation(summary = "Obtenir les détails d'un exercice")
    @GetMapping("/{exerciseId}")
    public ResponseEntity<ApiResponse<ExerciseResponse>> getExerciseDetails(
            @PathVariable Integer exerciseId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(exerciseService.getExerciseDetails(exerciseId, user.getId()));
    }

    @Operation(summary = "Soumettre une réponse à un exercice")
    @PostMapping("/{exerciseId}/submit")
    public ResponseEntity<ApiResponse<StudentExerciseResponse>> submitExercise(
            @PathVariable Integer exerciseId,
            @RequestBody StudentExerciseSubmissionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(exerciseService.submitExercise(exerciseId, user.getId(), request));
    }

    @Operation(summary = "Voir mes soumissions")
    @GetMapping("/my-submissions")
    public ResponseEntity<ApiResponse<List<StudentExerciseResponse>>> getMySubmissions(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(exerciseService.getMySubmissions(user.getId()));
    }
}
