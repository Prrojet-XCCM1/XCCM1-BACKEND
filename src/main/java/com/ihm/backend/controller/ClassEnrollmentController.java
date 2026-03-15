package com.ihm.backend.controller;

import com.ihm.backend.dto.ClassEnrollmentDTO;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.service.ClassEnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour la gestion des inscriptions aux classes de cours.
 *
 * - Étudiant : s'inscrire, se désinscrire, consulter ses inscriptions
 * - Enseignant : valider/rejeter des inscriptions, consulter les inscrits
 */
@Slf4j
@RestController
@RequestMapping("/api/class-enrollments")
@RequiredArgsConstructor
@Tag(name = "Inscriptions aux classes", description = "Inscription/désinscription et validation des étudiants dans les classes de cours")
public class ClassEnrollmentController {

    private final ClassEnrollmentService enrollmentService;

    // ─── ENDPOINTS ÉTUDIANTS ─────────────────────────────────────────────────

    /**
     * S'inscrire à une classe de cours (statut initial: PENDING)
     */
    @Operation(summary = "S'inscrire à une classe", description = "L'étudiant envoie une demande d'inscription. Elle sera en PENDING jusqu'à validation par l'enseignant.")
    @PostMapping("/{classId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ClassEnrollmentDTO>> enrollInClass(
            @PathVariable Long classId,
            Authentication authentication) throws Exception {
        User student = (User) authentication.getPrincipal();
        ClassEnrollmentDTO enrollment = enrollmentService.enrollInClass(classId, student.getId());
        return ResponseEntity.ok(ApiResponse.success("Demande d'inscription envoyée avec succès (en attente de validation)", enrollment));
    }

    /**
     * Récupérer toutes les inscriptions de l'étudiant connecté
     */
    @Operation(summary = "Mes inscriptions aux classes")
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<ClassEnrollmentDTO>>> getMyEnrollments(Authentication authentication) {
        User student = (User) authentication.getPrincipal();
        List<ClassEnrollmentDTO> enrollments = enrollmentService.getMyEnrollments(student.getId());
        return ResponseEntity.ok(ApiResponse.success("Inscriptions récupérées", enrollments));
    }

    /**
     * Récupérer l'inscription de l'étudiant pour une classe spécifique
     */
    @Operation(summary = "Mon inscription à une classe spécifique")
    @GetMapping("/class/{classId}/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ClassEnrollmentDTO>> getMyEnrollmentForClass(
            @PathVariable Long classId,
            Authentication authentication) {
        User student = (User) authentication.getPrincipal();
        ClassEnrollmentDTO enrollment = enrollmentService.getEnrollmentForClass(classId, student.getId());
        if (enrollment == null) {
            return ResponseEntity.ok(ApiResponse.success("Non inscrit à cette classe", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Inscription trouvée", enrollment));
    }

    /**
     * Se désinscrire d'une classe (toutes statuts confondus)
     */
    @Operation(summary = "Se désinscrire d'une classe", description = "Supprime l'inscription quelle que soit son statut (PENDING, APPROVED, REJECTED)")
    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> unenrollFromClass(
            @PathVariable Long enrollmentId,
            Authentication authentication) throws Exception {
        User student = (User) authentication.getPrincipal();
        enrollmentService.unenrollFromClass(enrollmentId, student.getId());
        return ResponseEntity.ok(ApiResponse.success("Désinscription réussie", null));
    }

    /**
     * Annuler une demande d'inscription en attente (PENDING uniquement)
     */
    @Operation(summary = "Annuler une demande d'inscription en attente")
    @DeleteMapping("/pending/{enrollmentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> cancelPendingEnrollment(
            @PathVariable Long enrollmentId,
            Authentication authentication) throws Exception {
        User student = (User) authentication.getPrincipal();
        enrollmentService.cancelPendingEnrollment(enrollmentId, student.getId());
        return ResponseEntity.ok(ApiResponse.success("Demande d'inscription annulée", null));
    }

    // ─── ENDPOINTS ENSEIGNANT ────────────────────────────────────────────────

    /**
     * Récupérer toutes les demandes PENDING pour les classes de l'enseignant
     */
    @Operation(summary = "Demandes d'inscription en attente (enseignant)")
    @GetMapping("/pending")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<ClassEnrollmentDTO>>> getPendingEnrollments(Authentication authentication) {
        User teacher = (User) authentication.getPrincipal();
        List<ClassEnrollmentDTO> pending = enrollmentService.getPendingForTeacher(teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Demandes en attente récupérées", pending));
    }

    /**
     * Récupérer tous les inscrits d'une classe (enseignant propriétaire)
     */
    @Operation(summary = "Liste des inscrits d'une classe (enseignant)")
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<ClassEnrollmentDTO>>> getClassEnrollments(
            @PathVariable Long classId,
            Authentication authentication) throws Exception {
        User teacher = (User) authentication.getPrincipal();
        List<ClassEnrollmentDTO> enrollments = enrollmentService.getClassEnrollments(classId, teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Inscrits de la classe récupérés", enrollments));
    }

    /**
     * Valider ou rejeter une demande d'inscription
     * status: APPROVED pour valider, REJECTED pour rejeter
     */
    @Operation(summary = "Valider ou rejeter une inscription",
               description = "Accessible seulement à l'enseignant propriétaire de la classe. status=APPROVED ou REJECTED")
    @PutMapping("/{enrollmentId}/validate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ClassEnrollmentDTO>> validateEnrollment(
            @PathVariable Long enrollmentId,
            @RequestParam EnrollmentStatus status,
            Authentication authentication) throws Exception {
        User teacher = (User) authentication.getPrincipal();
        ClassEnrollmentDTO updated = enrollmentService.validateEnrollment(enrollmentId, status, teacher.getId());
        String message = status == EnrollmentStatus.APPROVED
                ? "Étudiant validé avec succès"
                : "Étudiant refusé";
        return ResponseEntity.ok(ApiResponse.success(message, updated));
    }
}
