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

        switch (message.getType()) {
            case LOCK:
                if (lockService.acquireLock(message.getGranuleId(), userEmail)) {
                    messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                } else {
                    sendError(userEmail, "Verrou refusé pour l'élément " + message.getGranuleId());
                }
                break;

            case UNLOCK:
                lockService.releaseLock(message.getGranuleId(), userEmail);
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                if (message.getContent() != null) {
                    courseService.saveContentAsync(id, message.getContent());
                }
                break;

            case BLOCK_UPDATE:
                // Vérifier que l'utilisateur possède le verrou avant de broadcaster une mise à jour
                String blockOwner = lockService.getLockOwner(message.getGranuleId());
                if (blockOwner == null || !blockOwner.equals(userEmail)) {
                    sendError(userEmail, "Vous ne possédez pas le verrou pour l'élément " + message.getGranuleId());
                    return;
                }
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                // Persistance asynchrone du contenu à chaque mise à jour de bloc
                if (message.getContent() != null) {
                    courseService.saveContentAsync(id, message.getContent());
                }
                break;

            case MOVE:
            case CURSOR:
                // Vérifier si l'utilisateur possède le verrou avant de diffuser un MOVE
                if (message.getType() == CollaborationMessage.MessageType.MOVE) {
                    String moveOwner = lockService.getLockOwner(message.getGranuleId());
                    if (moveOwner == null || !moveOwner.equals(userEmail)) {
                        sendError(userEmail, "Vous ne possédez pas le verrou pour cet élément");
                        return;
                    }
                }
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                break;

            default:
                log.warn("Type de message non reconnu '{}' de {} sur le projet {}", message.getType(), userEmail, id);
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                break;
        }
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
