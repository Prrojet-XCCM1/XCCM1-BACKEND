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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gère les interactions sur les cours : likes, vues, commentaires.
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

    @PostMapping("/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CourseLikeResponse>> toggleLike(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User user) {
        CourseLikeResponse response = interactionService.toggleLike(courseId, user.getId());
        String message = response.isLiked() ? "Cours liké" : "Like retiré";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

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

    @PostMapping("/view")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CourseViewResponse>> recordView(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User user) {
        CourseViewResponse response = interactionService.recordView(courseId, user.getId());
        String message = response.isRecorded() ? "Vue enregistrée" : "Vue déjà comptabilisée";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    // ============================================================
    //  COMMENTAIRES
    // ============================================================

    /**
     * Liste publique des commentaires (pas d'authentification requise).
     * Pas de @PreAuthorize — la route est déjà ouverte dans SecurityConfig.
     */
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<List<CourseCommentDTO>>> getComments(
            @PathVariable Integer courseId) {
        List<CourseCommentDTO> comments = interactionService.getComments(courseId);
        return ResponseEntity.ok(ApiResponse.success("Commentaires récupérés", comments));
    }

    /**
     * Réservé à l'enseignant auteur du cours.
     */
    @GetMapping("/teacher/comments")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<CourseCommentDTO>>> getCourseCommentsForTeacher(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User user) throws Exception {
        List<CourseCommentDTO> comments = interactionService.getCourseCommentsForTeacher(courseId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(
                "Commentaires des étudiants récupérés (" + comments.size() + ")", comments));
    }

    @PostMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CourseCommentDTO>> addComment(
            @PathVariable Integer courseId,
            @Valid @RequestBody CourseCommentRequest request,
            @AuthenticationPrincipal User user) {
        CourseCommentDTO comment = interactionService.addComment(courseId, user.getId(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Commentaire ajouté", comment));
    }

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
     * Suppression par l'auteur.
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

    /**
     * Suppression par un admin (sans restriction de propriété).
     */
    @DeleteMapping("/comments/{commentId}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCommentAsAdmin(
            @PathVariable Integer courseId,
            @PathVariable Long commentId) {
        interactionService.deleteCommentAsAdmin(commentId);
        return ResponseEntity.ok(ApiResponse.success("Commentaire supprimé par l'admin", null));
    }
}
