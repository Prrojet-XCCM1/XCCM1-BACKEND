package com.ihm.backend.controller;

import com.ihm.backend.dto.EnrollmentDTO;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * Enrôler un étudiant à un cours
     * Accessible uniquement aux étudiants (ROLE_STUDENT)
     */
    @PostMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> enrollInCourse(
            @PathVariable Integer courseId,
            Authentication authentication) {
        try {
            User student = (User) authentication.getPrincipal();
            EnrollmentDTO enrollment = enrollmentService.enrollStudent(courseId, student.getId());
            return ResponseEntity.ok(ApiResponse.success("Enrôlement réussi", enrollment));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage(), null));
        } catch (Exception e) {
            log.error("Erreur lors de l'enrôlement", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Erreur lors de l'enrôlement", e.getMessage()));
        }
    }

    /**
     * Récupérer tous les cours enrôlés d'un étudiant
     */
    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyEnrollments(Authentication authentication) {
        try {
            User student = (User) authentication.getPrincipal();
            List<EnrollmentDTO> enrollments = enrollmentService.getUserEnrollments(student.getId());
            return ResponseEntity.ok(ApiResponse.success("Enrôlements récupérés", enrollments));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des enrôlements", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Erreur serveur", e.getMessage()));
        }
    }

    /**
     * Mettre à jour la progression d'un étudiant
     */
    @PutMapping("/{enrollmentId}/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateProgress(
            @PathVariable Long enrollmentId,
            @RequestParam Double progress,
            Authentication authentication) {
        try {
            EnrollmentDTO updated = enrollmentService.updateProgress(enrollmentId, progress);
            return ResponseEntity.ok(ApiResponse.success("Progression mise à jour", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage(), null));
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la progression", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Erreur serveur", e.getMessage()));
        }
    }

    /**
     * Marquer un cours comme complété
     */
    @PostMapping("/{enrollmentId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> markAsCompleted(
            @PathVariable Long enrollmentId,
            Authentication authentication) {
        try {
            EnrollmentDTO completed = enrollmentService.markAsCompleted(enrollmentId);
            return ResponseEntity.ok(ApiResponse.success("Cours marqué comme complété", completed));
        } catch (Exception e) {
            log.error("Erreur lors du marquage comme complété", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Erreur serveur", e.getMessage()));
        }
    }

    /**
     * Récupérer l'enrôlement d'un utilisateur pour un cours spécifique
     */
    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getEnrollmentForCourse(
            @PathVariable Integer courseId,
            Authentication authentication) {
        try {
            User student = (User) authentication.getPrincipal();
            EnrollmentDTO enrollment = enrollmentService.getEnrollmentForUser(courseId, student.getId());
            
            if (enrollment == null) {
                return ResponseEntity.ok(ApiResponse.success("Non enrôlé", null));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Enrôlement trouvé", enrollment));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'enrôlement", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("Erreur serveur", e.getMessage()));
        }
    }
}
