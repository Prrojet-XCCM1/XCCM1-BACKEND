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
     * Enrôler un étudiant ou un enseignant à un cours
     * Accessible aux étudiants ET aux enseignants
     */
    @PostMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> enrollInCourse(
            @PathVariable Integer courseId,
            Authentication authentication) throws Exception {
        User user = (User) authentication.getPrincipal();
        EnrollmentDTO enrollment = enrollmentService.enrollUser(courseId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Enrôlement réussi", enrollment));
    }

    /**
     * Récupérer tous les cours enrôlés de l'utilisateur connecté (étudiant ou enseignant)
     */
    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getMyEnrollments(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<EnrollmentDTO> enrollments = enrollmentService.getUserEnrollments(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Enrôlements récupérés", enrollments));
    }

    /**
     * Mettre à jour la progression
     */
    @PutMapping("/{enrollmentId}/progress")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> updateProgress(
            @PathVariable Long enrollmentId,
            @RequestParam Double progress,
            Authentication authentication) throws Exception {
        EnrollmentDTO updated = enrollmentService.updateProgress(enrollmentId, progress);
        return ResponseEntity.ok(ApiResponse.success("Progression mise à jour", updated));
    }

    /**
     * Marquer un cours comme complété
     */
    @PostMapping("/{enrollmentId}/complete")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> markAsCompleted(
            @PathVariable Long enrollmentId,
            Authentication authentication) throws Exception {
        EnrollmentDTO completed = enrollmentService.markAsCompleted(enrollmentId);
        return ResponseEntity.ok(ApiResponse.success("Cours marqué comme complété", completed));
    }

    /**
     * Récupérer l'enrôlement d'un utilisateur pour un cours spécifique
     */
    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> getEnrollmentForCourse(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        EnrollmentDTO enrollment = enrollmentService.getEnrollmentForUser(courseId, user.getId());

        if (enrollment == null) {
            return ResponseEntity.ok(ApiResponse.success("Non enrôlé", null));
        }

        return ResponseEntity.ok(ApiResponse.success("Enrôlement trouvé", enrollment));
    }

    /**
     * Valider ou rejeter un enrôlement
     * Accessible aux enseignants (ROLE_TEACHER)
     */
    @PutMapping("/{enrollmentId}/validate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> validateEnrollment(
            @PathVariable Long enrollmentId,
            @RequestParam com.ihm.backend.enums.EnrollmentStatus status,
            Authentication authentication) throws Exception {
        User teacher = (User) authentication.getPrincipal();
        EnrollmentDTO validated = enrollmentService.validateEnrollment(enrollmentId, status, teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Statut de l'enrôlement mis à jour", validated));
    }

    /**
     * Récupérer les enrôlements en attente pour les cours de l'enseignant connecté
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getPendingEnrollments(Authentication authentication) {
        User teacher = (User) authentication.getPrincipal();
        List<EnrollmentDTO> pending = enrollmentService.getPendingEnrollmentsForTeacher(teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Enrôlements en attente récupérés", pending));
    }

    /**
     * Se désenrôler d'un cours
     */
    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> unenroll(
            @PathVariable Long enrollmentId,
            Authentication authentication) throws Exception {
        User user = (User) authentication.getPrincipal();
        enrollmentService.unenroll(enrollmentId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Désenrôlement réussi", null));
    }

    /**
     * Annuler une demande d'enrôlement en attente
     */
    @DeleteMapping("/pending/{enrollmentId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> cancelPendingEnrollment(
            @PathVariable Long enrollmentId,
            Authentication authentication) throws Exception {
        User user = (User) authentication.getPrincipal();
        enrollmentService.cancelPendingEnrollment(enrollmentId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Demande d'enrôlement annulée", null));
    }

    /**
     * Inviter un utilisateur à collaborer sur un cours
     */
    @PostMapping("/invite")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> inviteUser(
            @RequestBody @jakarta.validation.Valid com.ihm.backend.dto.request.InvitationRequest request,
            Authentication authentication) throws Exception {
        User inviter = (User) authentication.getPrincipal();
        EnrollmentDTO invitation = enrollmentService.inviteUser(request.getCourseId(), request.getEmail(), inviter.getId());
        return ResponseEntity.ok(ApiResponse.success("Invitation envoyée avec succès", invitation));
    }
}
