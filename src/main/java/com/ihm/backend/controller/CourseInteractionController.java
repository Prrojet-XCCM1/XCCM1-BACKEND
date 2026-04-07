package com.ihm.backend.controller;

import com.ihm.backend.dto.CourseCommentDTO;
import com.ihm.backend.dto.request.CourseCommentRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.CourseLikeResponse;
import com.ihm.backend.dto.response.CourseViewResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.service.CourseInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gère les interactions des utilisateurs sur les cours :
 * - Likes (toggle)
 * - Vues (enregistrement unique)
 * - Commentaires (CRUD)
 *
 * Base URL : /api/courses/{courseId}/interactions
 */
@Slf4j
@RestController
@RequestMapping("/api/courses/{courseId}/interactions")
@RequiredArgsConstructor
public class CourseInteractionController {

    private final CourseInteractionService interactionService;

    // ============================================================
    //  LIKES
    // ============================================================

    /**
     * Toggle like / unlike d'un cours.
     * Accessible à tout utilisateur authentifié (étudiant ou enseignant).
     */
    @PostMapping("/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CourseLikeResponse>> toggleLike(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User user) throws Exception {
        CourseLikeResponse response = interactionService.toggleLike(courseId, user.getId());
        String message = response.isLiked() ? "Cours liké avec succès" : "Like retiré avec succès";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    /**
     * Récupère le statut du like de l'utilisateur connecté pour un cours.
     */
    @GetMapping("/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CourseLikeResponse>> getLikeStatus(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User user) {
        CourseLikeResponse response = interactionService.getLikeStatus(courseId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Statut du like récupéré", response));
    }

    // ============================================================
    //  VUES
    // ============================================================

    /**
     * Enregistre la vue de l'utilisateur sur un cours (une seule fois par user).
     * Accessible à tout utilisateur authentifié.
     */
    @PostMapping("/view")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CourseViewResponse>> recordView(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User user) throws Exception {
        CourseViewResponse response = interactionService.recordView(courseId, user.getId());
        String message = response.isRecorded() ? "Vue enregistrée" : "Vue déjà comptabilisée";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    // ============================================================
    //  COMMENTAIRES
    // ============================================================

    /**
     * Récupère tous les commentaires d'un cours (public, sans authentification requise).
     */
    @GetMapping("/comments")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<List<CourseCommentDTO>>> getComments(
            @PathVariable Integer courseId) {
        List<CourseCommentDTO> comments = interactionService.getComments(courseId);
        return ResponseEntity.ok(ApiResponse.success("Commentaires récupérés", comments));
    }

    /**
     * Endpoint réservé à l'enseignant : récupère tous les commentaires des étudiants
     * sur un cours dont il est l'auteur.
     * GET /api/courses/{courseId}/interactions/teacher/comments
     */
    @GetMapping("/teacher/comments")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<CourseCommentDTO>>> getCourseCommentsForTeacher(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User user) throws Exception {
        List<CourseCommentDTO> comments = interactionService.getCourseCommentsForTeacher(courseId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(
                "Commentaires des étudiants récupérés (" + comments.size() + " au total)", comments));
    }

    /**
     * Ajoute un commentaire sur un cours.
     * Accessible à tout utilisateur authentifié (étudiant ou enseignant).
     */
    @PostMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CourseCommentDTO>> addComment(
            @PathVariable Integer courseId,
            @Valid @RequestBody CourseCommentRequest request,
            @AuthenticationPrincipal User user) throws Exception {
        CourseCommentDTO comment = interactionService.addComment(courseId, user.getId(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Commentaire ajouté", comment));
    }

    /**
     * Modifie un commentaire existant.
     * Seul l'auteur du commentaire peut le modifier.
     */
    @PutMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CourseCommentDTO>> updateComment(
            @PathVariable Integer courseId,
            @PathVariable Long commentId,
            @Valid @RequestBody CourseCommentRequest request,
            @AuthenticationPrincipal User user) throws Exception {
        CourseCommentDTO updated = interactionService.updateComment(commentId, user.getId(), request.getContent());
        return ResponseEntity.ok(ApiResponse.success("Commentaire mis à jour", updated));
    }

    /**
     * Supprime un commentaire.
     * Seul l'auteur du commentaire peut le supprimer.
     */
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Integer courseId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user) throws Exception {
        interactionService.deleteComment(commentId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Commentaire supprimé", null));
    }
}
