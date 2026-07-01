package com.ihm.backend.controller;

import com.ihm.backend.service.CourseYjsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints appelés uniquement par le serveur Hocuspocus, jamais par le frontend.
 * Protégés par une clé API interne (X-Internal-Key header), pas par JWT.
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalCourseController {

    private final CourseYjsService courseYjsService;

    @Value("${xccm.internal.api-key}")
    private String internalApiKey;

    private boolean isAuthorized(String key) {
        return internalApiKey != null && internalApiKey.equals(key);
    }

    @GetMapping("/courses/{courseId}/yjs-state")
    public ResponseEntity<byte[]> getYjsState(
            @PathVariable Integer courseId,
            @RequestHeader(value = "X-Internal-Key", required = false) String internalKey) {

        if (!isAuthorized(internalKey)) {
            log.warn("Tentative d'accès interne non autorisé à /api/internal/courses/{}/yjs-state", courseId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        byte[] state = courseYjsService.getYjsState(courseId);
        if (state == null || state.length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(state);
    }

    @PutMapping(value = "/courses/{courseId}/yjs-state",
                consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> saveYjsState(
            @PathVariable Integer courseId,
            @RequestHeader(value = "X-Internal-Key", required = false) String internalKey,
            @RequestBody byte[] state) {

        if (!isAuthorized(internalKey)) {
            log.warn("Tentative d'accès interne non autorisé à PUT /api/internal/courses/{}/yjs-state", courseId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        courseYjsService.saveYjsState(courseId, state);
        return ResponseEntity.ok().build();
    }
}
