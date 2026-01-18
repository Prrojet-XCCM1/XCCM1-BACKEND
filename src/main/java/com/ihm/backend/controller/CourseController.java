package com.ihm.backend.controller;

import com.ihm.backend.dto.request.CourseCreateRequest;
import com.ihm.backend.dto.request.CourseUpdateRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.CourseResponse;
import com.ihm.backend.dto.response.EnrichedCourseResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{authorId}")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(@RequestBody CourseCreateRequest request,
            @PathVariable UUID authorId,
            Authentication authentication) throws Exception {
        // Vérifier que l'enseignant crée un cours pour lui-même
        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getId().equals(authorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.forbidden("Vous ne pouvez créer un cours que pour vous-même", null));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Cours créé avec succès", courseService.createCourse(request, authorId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{authorId}")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAuthorCourses(@PathVariable UUID authorId,
            Authentication authentication) throws Exception {
        // Vérifier que l'enseignant accède à ses propres cours
        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getId().equals(authorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.forbidden("Vous ne pouvez consulter que vos propres cours", null));
        }

        return ResponseEntity.ok(
                ApiResponse.success("Cours récupérés avec succès", courseService.getAllCoursesForTeacher(authorId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{courseId}/coverImage/upload")
    public ResponseEntity<ApiResponse<CourseResponse>> uploadImage(@PathVariable Integer courseId,
            @RequestParam MultipartFile image,
            Authentication authentication) throws Exception {
        User currentUser = (User) authentication.getPrincipal();
        courseService.validateOwnership(courseId, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("Image de couverture téléchargée avec succès",
                courseService.uploadCoverImage(courseId, image)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{courseId}/setStatus/{status}")
    public ResponseEntity<ApiResponse<CourseResponse>> changeCourseStatus(@PathVariable Integer courseId,
            @PathVariable CourseStatus status,
            Authentication authentication) throws Exception {
        User currentUser = (User) authentication.getPrincipal();
        courseService.validateOwnership(courseId, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("Statut du cours mis à jour avec succès",
                courseService.changeCourseStatus(status, courseId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/{courseId}/status")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourseStatus(@PathVariable Integer courseId,
            @RequestParam CourseStatus status,
            Authentication authentication) throws Exception {
        User currentUser = (User) authentication.getPrincipal();
        courseService.validateOwnership(courseId, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("Statut du cours mis à jour avec succès",
                courseService.changeCourseStatus(status, courseId)));
    }

    @GetMapping("/{authorId}/status/{status}")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCoureByStatusForAuthor(@PathVariable Integer authorId,
            @PathVariable CourseStatus status) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Cours récupérés avec succès",
                courseService.getCoursesByStatusForAuthor(authorId, status)));
    }

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllCourses() {
        return ResponseEntity
                .ok(ApiResponse.success("Tous les cours récupérés avec succès", courseService.getAllCourses()));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(@PathVariable Integer courseId,
            @RequestBody CourseUpdateRequest request,
            Authentication authentication) throws Exception {
        User currentUser = (User) authentication.getPrincipal();
        courseService.validateOwnership(courseId, currentUser.getId());

        return ResponseEntity
                .ok(ApiResponse.success("Cours mis à jour avec succès", courseService.updateCourse(courseId, request)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Integer courseId,
            Authentication authentication) throws Exception {
        User currentUser = (User) authentication.getPrincipal();
        courseService.validateOwnership(courseId, currentUser.getId());

        courseService.deleteCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success("Cours supprimé avec succès"));
    }

    /**
     * Récupérer tous les cours enrichis avec les enrôlements de l'utilisateur
     * courant
     * Accessible à tous les utilisateurs authentifiés
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/enriched")
    public ResponseEntity<ApiResponse<List<EnrichedCourseResponse>>> getEnrichedCourses(Authentication authentication) {
        UUID userId = null;
        if (authentication != null) {
            User currentUser = (User) authentication.getPrincipal();
            userId = currentUser.getId();
        }

        List<EnrichedCourseResponse> enrichedCourses = courseService.getEnrichedCourses(userId);
        return ResponseEntity.ok(ApiResponse.success("Cours enrichis récupérés avec succès", enrichedCourses));
    }

    /**
     * Récupérer un cours enrichi spécifique
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/enriched/{courseId}")
    public ResponseEntity<ApiResponse<EnrichedCourseResponse>> getEnrichedCourse(@PathVariable Integer courseId,
            Authentication authentication) throws Exception {
        UUID userId = null;
        if (authentication != null) {
            User currentUser = (User) authentication.getPrincipal();
            userId = currentUser.getId();
        }

        EnrichedCourseResponse enrichedCourse = courseService.getEnrichedCourse(courseId, userId);
        return ResponseEntity.ok(ApiResponse.success("Cours enrichi récupéré avec succès", enrichedCourse));
    }


    @PreAuthorize("permitAll()")
    @PostMapping("/{courseId}/view")
    public ResponseEntity<ApiResponse<CourseResponse>> incrementViewCount(@PathVariable Integer courseId) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Nombre de vues incrémenté", 
                courseService.incrementViewCount(courseId)));
    }

    @PreAuthorize("permitAll()")
    @PostMapping("/{courseId}/like")
    public ResponseEntity<ApiResponse<CourseResponse>> incrementLikeCount(@PathVariable Integer courseId) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Nombre de likes incrémenté", 
                courseService.incrementLikeCount(courseId)));
    }



   

    @PreAuthorize("permitAll()")
    @PostMapping("/{courseId}/like")
    public ResponseEntity<ApiResponse<CourseResponse>> decrementLikeCount(@PathVariable Integer courseId) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Nombre de likes décrémenté", 
                courseService.decrementLikeCount(courseId)));
    }

    @PreAuthorize("permitAll()")
    @PostMapping("/{courseId}/download")
    public ResponseEntity<ApiResponse<CourseResponse>> incrementDownloadCount(@PathVariable Integer courseId) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Nombre de téléchargements incrémenté", 
                courseService.incrementDownloadCount(courseId)));
    }
}
