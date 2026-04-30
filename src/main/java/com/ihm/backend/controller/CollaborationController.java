package com.ihm.backend.controller;

import com.ihm.backend.dto.websocket.CollaborationMessage;
import com.ihm.backend.entity.Exercise;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.repository.jpa.EnrollmentRepository;
import com.ihm.backend.repository.jpa.ExerciseRepository;
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
    private final ExerciseRepository exerciseRepository;
    private final EnrollmentRepository enrollmentRepository;

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
    public void saveContentAsync(Integer exerciseId, String content) {
        log.info("Saving content for exercise {} asynchronously", exerciseId);
        exerciseRepository.findById(exerciseId).ifPresent(exercise -> {
            exercise.setContent(content);
            exerciseRepository.save(exercise);
            log.info("Exercise {} saved successfully", exerciseId);
        });
    }

    private boolean isAuthorized(Integer exerciseId, User user) {
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(exerciseId);
        if (exerciseOpt.isEmpty()) return false;
        
        Exercise exercise = exerciseOpt.get();
        if (exercise.getCourse() == null) return false;

        // Est-ce l'auteur ?
        if (exercise.getCourse().getAuthor().getEmail().equals(user.getEmail())) {
            return true;
        }

        // Est-ce un collaborateur inscrit/invité ?
        return enrollmentRepository.findByCourse_IdAndUser_Id(
                exercise.getCourse().getId(), 
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


