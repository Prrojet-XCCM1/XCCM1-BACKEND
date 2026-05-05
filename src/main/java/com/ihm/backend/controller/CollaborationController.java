package com.ihm.backend.controller;

import com.ihm.backend.dto.websocket.CollaborationMessage;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.repository.jpa.CourseRepository;
import com.ihm.backend.repository.jpa.EnrollmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ihm.backend.service.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CollaborationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final LockService lockService;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ObjectMapper objectMapper;

    @MessageMapping("/projet/{id}/action")
    public void handleAction(@DestinationVariable Integer id, @Payload CollaborationMessage message, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            log.error("Unauthenticated user tried to send an action");
            return;
        }

        User user = (User) authentication.getPrincipal();
        String userEmail = user.getEmail();
        
        // --- Vérification de l'Autorisation ---
        if (!isAuthorized(id, user)) {
            sendError(userEmail, "Non autorisé à modifier ce document");
            return;
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
                // Sauvegarde asynchrone si du contenu est envoyé
                if (message.getContent() != null) {
                    saveContentAsync(id, message.getContent());
                }
                break;

            case MOVE:
            case CURSOR:
                // Vérifier si l'utilisateur possède le verrou avant de diffuser un MOVE
                if (message.getType() == CollaborationMessage.MessageType.MOVE) {
                    String owner = lockService.getLockOwner(message.getGranuleId());
                    if (owner == null || !owner.equals(userEmail)) {
                        sendError(userEmail, "Vous ne possédez pas le verrou pour cet élément");
                        return;
                    }
                }
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                break;

            default:
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

    @Async
    public void saveContentAsync(Integer courseId, String content) {
        log.info("Saving content for course {} asynchronously", courseId);
        courseRepository.findById(courseId).ifPresent(course -> {
            try {
                // Convert string content to Map for the JSON column
                java.util.Map<String, Object> contentMap = objectMapper.readValue(content, new TypeReference<java.util.Map<String, Object>>() {});
                course.setContent(contentMap);
                courseRepository.save(course);
                log.info("Course {} content saved successfully", courseId);
            } catch (Exception e) {
                log.error("Failed to parse or save course content for course {}: {}", courseId, e.getMessage());
            }
        });
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


