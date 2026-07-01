package com.ihm.backend.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ihm.backend.dto.websocket.CollaborationMessage;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.repository.jpa.CourseRepository;
import com.ihm.backend.repository.jpa.EnrollmentRepository;
import com.ihm.backend.service.CourseService;
import com.ihm.backend.service.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CollaborationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final LockService lockService;
    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Cache d'autorisation "email:courseId" → true.
     * TTL de 5 minutes : les révocations d'accès sont prises en compte dans ce délai.
     * Taille max de 10 000 entrées pour éviter toute fuite mémoire.
     */
    private final Cache<String, Boolean> authorizedCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    @MessageMapping("/projet/{id}/action")
    public void handleAction(@DestinationVariable Integer id, @Payload CollaborationMessage message, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            log.error("Unauthenticated user tried to send an action");
            return;
        }

        User user = (User) authentication.getPrincipal();
        String userEmail = user.getEmail();

        // Vérifier l'autorisation avec cache Caffeine (TTL 5 min, max 10 000 entrées)
        String cacheKey = userEmail + ":" + id;
        if (authorizedCache.getIfPresent(cacheKey) == null) {
            if (!isAuthorized(id, user)) {
                sendError(userEmail, "Non autorisé à modifier ce document");
                return;
            }
            authorizedCache.put(cacheKey, Boolean.TRUE);
        }

        message.setSenderEmail(userEmail);
        message.setSenderName(user.getFullName());

        // Extraire le vrai nodeId (le frontend envoie granuleId=0 pour LOCK/UNLOCK/BLOCK_UPDATE,
        // le vrai identifiant est dans payload.nodeId ou message.nodeId selon le type d'action)
        String nodeId = extractNodeId(message);

        switch (message.getType()) {
            case LOCK:
                if (nodeId == null) {
                    // Pas de nœud identifié : diffuser sans verrou
                    messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                    return;
                }
                if (lockService.acquireLock(id, nodeId, userEmail)) {
                    messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                } else {
                    String owner = lockService.getLockOwner(id, nodeId);
                    sendError(userEmail, "Nœud verrouillé par: " + (owner != null ? owner : "un collaborateur"));
                }
                break;

            case UNLOCK:
                if (nodeId != null) {
                    lockService.releaseLock(id, nodeId, userEmail);
                }
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                if (message.getContent() != null) {
                    courseService.saveContentAsync(id, message.getContent());
                }
                break;

            case BLOCK_UPDATE:
                // Vérifier qu'aucun autre utilisateur ne possède le verrou sur ce nœud
                if (nodeId != null) {
                    String blockOwner = lockService.getLockOwner(id, nodeId);
                    if (blockOwner != null && !blockOwner.equals(userEmail)) {
                        sendError(userEmail, "Ce nœud est en cours d'édition par un collaborateur");
                        return;
                    }
                }
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                break;

            case MOVE:
                // Opération structurelle : diffusée sans vérification de verrou de contenu
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                break;

            case DELETE:
            case RENAME:
            case DUPLICATE:
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                break;

            case CURSOR:
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                break;

            default:
                log.warn("Type de message non reconnu '{}' de {} sur le projet {}", message.getType(), userEmail, id);
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                break;
        }
    }

    /**
     * Extrait le nodeId réel du message. Priorité: granuleId explicite et != "0"
     * > payload.nodeId > message.nodeId. Ignore les valeurs null/vides/"0" envoyées
     * par le frontend actuel pour LOCK/UNLOCK/BLOCK_UPDATE.
     */
    private String extractNodeId(CollaborationMessage message) {
        if (message.getGranuleId() != null
                && !message.getGranuleId().isEmpty()
                && !message.getGranuleId().equals("0")) {
            return message.getGranuleId();
        }

        if (message.getPayload() instanceof Map<?, ?> payloadMap) {
            Object nodeId = payloadMap.get("nodeId");
            if (nodeId != null && !String.valueOf(nodeId).equals("0") && !String.valueOf(nodeId).isEmpty()) {
                return String.valueOf(nodeId);
            }
        }

        if (message.getNodeId() != null && !message.getNodeId().isEmpty()) {
            return message.getNodeId();
        }

        return null;
    }

    private void sendError(String userEmail, String errorMessage) {
        CollaborationMessage error = CollaborationMessage.builder()
                .type(CollaborationMessage.MessageType.ERROR)
                .content(errorMessage)
                .build();
        messagingTemplate.convertAndSendToUser(userEmail, "/queue/errors", error);
    }

    private boolean isAuthorized(Integer courseId, User user) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) return false;

        Course course = courseOpt.get();

        // Est-ce l'auteur ?
        if (course.getAuthor() != null && course.getAuthor().getEmail().equals(user.getEmail())) {
            return true;
        }

        // Est-ce un éditeur (collaborateur direct) ?
        if (course.getEditors() != null &&
            course.getEditors().stream().anyMatch(u -> u.getEmail().equals(user.getEmail()))) {
            return true;
        }

        // Est-ce un collaborateur inscrit/invité via enrôlement ?
        return enrollmentRepository.findByCourse_IdAndUser_Id(
                course.getId(),
                user.getId()
        ).map(enrollment ->
                enrollment.getStatus() == EnrollmentStatus.APPROVED ||
                enrollment.getStatus() == EnrollmentStatus.INVITED
        ).orElse(false);
    }

    @MessageExceptionHandler
    public void handleException(Exception exception, Authentication authentication) {
        log.error("WebSocket error: {}", exception.getMessage(), exception);
        if (authentication != null) {
            sendError(authentication.getName(), "Erreur serveur : " + exception.getMessage());
        }
    }
}
