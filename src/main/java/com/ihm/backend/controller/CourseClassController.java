package com.ihm.backend.controller;

import com.ihm.backend.dto.request.CourseClassCreateRequest;
import com.ihm.backend.dto.request.CourseClassUpdateRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.CourseClassResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.ClassStatus;
import com.ihm.backend.service.CourseClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST pour la gestion des Classes de Cours (Salles de cours).
 *
 * Un enseignant crée et gère ses salles, regroupe ses cours par thématique.
 * Un étudiant peut lister et consulter les classes disponibles.
 */
@Slf4j
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Classes de cours", description = "Gestion des salles/classes de cours thématiques")
public class CourseClassController {

    private final CourseClassService courseClassService;

    // ─── ENDPOINTS ENSEIGNANT ────────────────────────────────────────────────

    /**
     * Créer une nouvelle classe de cours
     */
    @Operation(summary = "Créer une classe de cours", description = "Accessible uniquement aux enseignants")
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<CourseClassResponse>> createClass(
            @RequestBody CourseClassCreateRequest request,
            Authentication authentication) {
        User teacher = (User) authentication.getPrincipal();
        CourseClassResponse response = courseClassService.createClass(request, teacher.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Classe créée avec succès", response));
    }

    /**
     * Récupérer les classes de l'enseignant connecté
     */
    @Operation(summary = "Mes classes (enseignant)")
    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<CourseClassResponse>>> getMyClasses(Authentication authentication) {
        User teacher = (User) authentication.getPrincipal();
        List<CourseClassResponse> classes = courseClassService.getMyClasses(teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Classes récupérées avec succès", classes));
    }

    /**
     * Modifier une classe (owner uniquement)
     */
    @Operation(summary = "Modifier une classe de cours")
    @PutMapping("/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<CourseClassResponse>> updateClass(
            @PathVariable Long classId,
            @RequestBody CourseClassUpdateRequest request,
            Authentication authentication) throws Exception {
        User teacher = (User) authentication.getPrincipal();
        CourseClassResponse response = courseClassService.updateClass(classId, request, teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Classe mise à jour avec succès", response));
    }

    /**
     * Supprimer une classe (owner uniquement)
     */
    @Operation(summary = "Supprimer une classe de cours")
    @DeleteMapping("/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteClass(
            @PathVariable Long classId,
            Authentication authentication) throws Exception {
        User teacher = (User) authentication.getPrincipal();
        courseClassService.deleteClass(classId, teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Classe supprimée avec succès", null));
    }

    /**
     * Changer le statut d'une classe (OPEN / CLOSED / ARCHIVED)
     */
    @Operation(summary = "Changer le statut d'une classe", description = "OPEN = inscriptions ouvertes, CLOSED = fermée, ARCHIVED = archivée")
    @PatchMapping("/{classId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<CourseClassResponse>> changeStatus(
            @PathVariable Long classId,
            @RequestParam ClassStatus status,
            Authentication authentication) throws Exception {
        User teacher = (User) authentication.getPrincipal();
        CourseClassResponse response = courseClassService.changeStatus(classId, status, teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour vers " + status, response));
    }

    /**
     * Ajouter un cours à une classe
     */
    @Operation(summary = "Ajouter un cours à la classe")
    @PostMapping("/{classId}/courses/{courseId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<CourseClassResponse>> addCourse(
            @PathVariable Long classId,
            @PathVariable Integer courseId,
            Authentication authentication) throws Exception {
        User teacher = (User) authentication.getPrincipal();
        CourseClassResponse response = courseClassService.addCourseToClass(classId, courseId, teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Cours ajouté à la classe avec succès", response));
    }

    /**
     * Retirer un cours d'une classe
     */
    @Operation(summary = "Retirer un cours de la classe")
    @DeleteMapping("/{classId}/courses/{courseId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<CourseClassResponse>> removeCourse(
            @PathVariable Long classId,
            @PathVariable Integer courseId,
            Authentication authentication) throws Exception {
        User teacher = (User) authentication.getPrincipal();
        CourseClassResponse response = courseClassService.removeCourseFromClass(classId, courseId, teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Cours retiré de la classe", response));
    }

    /**
     * Upload une image de couverture pour la classe
     */
    @Operation(summary = "Uploader l'image de couverture d'une classe")
    @PostMapping("/{classId}/cover")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<CourseClassResponse>> uploadCoverImage(
            @PathVariable Long classId,
            @RequestParam("image") MultipartFile image,
            Authentication authentication) throws Exception {
        User teacher = (User) authentication.getPrincipal();
        CourseClassResponse response = courseClassService.uploadCoverImage(classId, image, teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Image de couverture mise à jour", response));
    }

    // ─── ENDPOINTS PUBLICS / ÉTUDIANTS ───────────────────────────────────────

    /**
     * Lister toutes les classes OPEN (accessible à tous)
     * Si un étudiant est connecté, enrichit avec son statut d'inscription
     */
    @Operation(summary = "Lister les classes disponibles (OPEN)", description = "Accessible à tous. Si étudiant connecté, retourne son statut d'inscription.")
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<List<CourseClassResponse>>> getAllOpenClasses(Authentication authentication) {
        UUID userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userId = ((User) authentication.getPrincipal()).getId();
        }
        List<CourseClassResponse> classes = courseClassService.getAllOpenClasses(userId);
        return ResponseEntity.ok(ApiResponse.success("Classes disponibles récupérées", classes));
    }

    /**
     * Récupérer le détail d'une classe par ID
     */
    @Operation(summary = "Détail d'une classe de cours")
    @GetMapping("/{classId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<CourseClassResponse>> getClassById(
            @PathVariable Long classId,
            Authentication authentication) {
        UUID userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userId = ((User) authentication.getPrincipal()).getId();
        }
        CourseClassResponse response = courseClassService.getClassById(classId, userId);
        return ResponseEntity.ok(ApiResponse.success("Classe récupérée avec succès", response));
    }
}
