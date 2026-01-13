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
}
