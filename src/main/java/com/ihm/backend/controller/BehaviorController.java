package com.ihm.backend.controller;

import com.ihm.backend.dto.BehaviorEventRequest;
import com.ihm.backend.entity.StudentBehaviorEvent;
import com.ihm.backend.entity.User;
import com.ihm.backend.repository.StudentBehaviorEventRepository;
import com.ihm.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur pour l'enregistrement des comportements des étudiants.
 *
 * Endpoints:
 *   POST /api/v1/behavior/log    → Enregistrer un événement comportemental
 *   GET  /api/v1/behavior/{studentId} → Récupérer l'historique comportemental
 */
@RestController
@RequestMapping("/api/v1/behavior")
@RequiredArgsConstructor
public class BehaviorController {

    private final StudentBehaviorEventRepository behaviorRepository;
    private final UserRepository userRepository;

    /**
     * Enregistre un événement comportemental émis par le frontend.
     * Appelé automatiquement par le hook useTracking (Frontend).
     */
    @PostMapping("/log")
    public ResponseEntity<?> logBehaviorEvent(
            @RequestBody BehaviorEventRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User student = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        StudentBehaviorEvent event = StudentBehaviorEvent.builder()
                .student(student)
                .eventType(request.getEventType())
                .granuleId(request.getGranuleId())
                .notion(request.getNotion())
                .rawScore(request.getRawScore())
                .durationSeconds(request.getDurationSeconds())
                .readDepthPercent(request.getReadDepthPercent())
                .metadata(request.getMetadata())
                .build();

        behaviorRepository.save(event);

        return ResponseEntity.ok().body("{\"message\": \"Event logged successfully\"}");
    }

    /**
     * Récupère l'historique comportemental d'un étudiant.
     * Utilisé par le LLM Service pour calculer le niveau de connaissance.
     */
    @GetMapping("/{studentId}")
    public ResponseEntity<List<StudentBehaviorEvent>> getStudentBehavior(
            @PathVariable UUID studentId) {
        List<StudentBehaviorEvent> events = behaviorRepository
                .findByStudentIdOrderByOccurredAtDesc(studentId);
        return ResponseEntity.ok(events);
    }
}
