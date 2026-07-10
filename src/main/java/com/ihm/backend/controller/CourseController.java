package com.ihm.backend.controller;

import com.ihm.backend.dto.request.CourseAIGenerateRequest;
import com.ihm.backend.dto.request.CourseCreateRequest;
import com.ihm.backend.dto.request.CourseUpdateRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.CourseGenerationJobCreatedResponse;
import com.ihm.backend.dto.response.CourseGenerationJobResponse;
import com.ihm.backend.dto.response.CourseResponse;
import com.ihm.backend.dto.response.EnrichedCourseResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.service.CourseService;
import com.ihm.backend.service.CourseGenerationJobService;
import com.ihm.backend.service.LLMIndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseGenerationJobService courseGenerationJobService;

    @Autowired
    private LLMIndexingService llmIndexingService;

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{authorId}")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(@RequestBody CourseCreateRequest request,
            @PathVariable UUID authorId,
            @AuthenticationPrincipal User currentUser) throws Exception {
        // Vérifier que l'enseignant crée un cours pour lui-même
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
            @AuthenticationPrincipal User currentUser) throws Exception {
        // Vérifier que l'enseignant accède à ses propres cours
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
            @AuthenticationPrincipal User currentUser) throws Exception {
        courseService.validateOwnership(courseId, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("Image de couverture téléchargée avec succès",
                courseService.uploadCoverImage(courseId, image)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/{courseId}/status")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourseStatus(@PathVariable Integer courseId,
            @RequestParam CourseStatus status,
            @AuthenticationPrincipal User currentUser) throws Exception {
        courseService.validateOwnership(courseId, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("Statut du cours mis à jour avec succès",
                courseService.changeCourseStatus(status, courseId)));
    }

    @GetMapping("/{authorId}/status/{status}")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCoureByStatusForAuthor(@PathVariable UUID authorId,
            @PathVariable CourseStatus status) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Cours récupérés avec succès",
                courseService.getCoursesByStatusForAuthor(authorId, status)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllCourses() {
        return ResponseEntity
                .ok(ApiResponse.success("Tous les cours récupérés avec succès", courseService.getAllCourses()));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(@PathVariable Integer courseId,
            @RequestBody CourseUpdateRequest request,
            @AuthenticationPrincipal User currentUser) throws Exception {
        courseService.validateOwnership(courseId, currentUser.getId());

        return ResponseEntity
                .ok(ApiResponse.success("Cours mis à jour avec succès", courseService.updateCourse(courseId, request)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Integer courseId,
            @AuthenticationPrincipal User currentUser) throws Exception {
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
    public ResponseEntity<ApiResponse<List<EnrichedCourseResponse>>> getEnrichedCourses(@AuthenticationPrincipal User currentUser) {
        UUID userId = null;
        if (currentUser != null) {
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
            @AuthenticationPrincipal User currentUser) throws Exception {
        UUID userId = null;
        if (currentUser != null) {
            userId = currentUser.getId();
        }

        EnrichedCourseResponse enrichedCourse = courseService.getEnrichedCourse(courseId, userId);
        return ResponseEntity.ok(ApiResponse.success("Cours enrichi récupéré avec succès", enrichedCourse));
    }


    @PostMapping("/{courseId}/view")
    public ResponseEntity<ApiResponse<CourseResponse>> incrementViewCount(@PathVariable Integer courseId) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Nombre de vues incrémenté", 
                courseService.incrementViewCount(courseId)));
    }

    @PostMapping("/{courseId}/like")
    public ResponseEntity<ApiResponse<CourseResponse>> incrementLikeCount(@PathVariable Integer courseId) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Nombre de likes incrémenté", 
                courseService.incrementLikeCount(courseId)));
    }



   

    @PostMapping("/{courseId}/dislike")
    public ResponseEntity<ApiResponse<CourseResponse>> decrementLikeCount(@PathVariable Integer courseId) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Nombre de likes décrémenté", 
                courseService.decrementLikeCount(courseId)));
    }

    @PostMapping("/{courseId}/download")
    public ResponseEntity<ApiResponse<CourseResponse>> incrementDownloadCount(@PathVariable Integer courseId) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Nombre de téléchargements incrémenté", 
                courseService.incrementDownloadCount(courseId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping(value = "/generate-ai", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateCourseWithAI(
            @RequestBody CourseAIGenerateRequest request,
            @AuthenticationPrincipal User currentUser,
            HttpServletResponse response) {

        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");

        // Timeout 6 min (aligné avec le WebClient côté LLMIndexingService)
        SseEmitter emitter = new SseEmitter(360_000L);

        // Virtual Threads (Java 21) : pas de thread bloqué pendant le streaming
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        executor.submit(() -> llmIndexingService.streamGenerateCourse(request, emitter, request.getCourseId()));
        executor.shutdown();

        return emitter;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/generate-ai/jobs")
    public ResponseEntity<ApiResponse<CourseGenerationJobCreatedResponse>> startCourseGenerationJob(
            @RequestBody CourseAIGenerateRequest request,
            @AuthenticationPrincipal User currentUser) {
        CourseGenerationJobCreatedResponse created = courseGenerationJobService.startJob(currentUser.getId(), request);
        ApiResponse<CourseGenerationJobCreatedResponse> body = ApiResponse.<CourseGenerationJobCreatedResponse>builder()
                .code(202)
                .success(true)
                .message("Job de génération IA lancé")
                .data(created)
                .build();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/generate-ai/jobs/{jobId}")
    public ResponseEntity<ApiResponse<CourseGenerationJobResponse>> getCourseGenerationJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal User currentUser) {
        CourseGenerationJobResponse job = courseGenerationJobService.getJob(jobId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Statut du job récupéré", job));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecommendations(
            @RequestBody(required = false) com.ihm.backend.dto.request.RecommendationRequest request) {
        if (request == null) {
            return ResponseEntity.ok(ApiResponse.success("Aucune donnée fournie", Collections.emptyList()));
        }
        return ResponseEntity.ok(ApiResponse.success("Recommandations récupérées",
                courseService.getRecommendations(request.getTitle(), request.getDescription(), request.getContent())));
    }

    /**
     * Ré-indexe (RAG) tous les cours déjà publiés. À déclencher une fois après
     * l'amélioration de l'indexation (contenu complet). Idempotent.
     */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindexPublishedCourses() {
        int count = courseService.reindexAllPublishedCourses();
        return ResponseEntity.ok(ApiResponse.success(
                "Ré-indexation lancée", Map.of("reindexed", count)));
    }
}
