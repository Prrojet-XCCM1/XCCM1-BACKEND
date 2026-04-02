package com.ihm.backend.controller;

import com.ihm.backend.dto.websocket.CollaborationMessage;
import com.ihm.backend.entity.Exercise;
import com.ihm.backend.repository.ExerciseRepository;
import com.ihm.backend.service.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CollaborationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final LockService lockService;
    private final ExerciseRepository exerciseRepository;

    @MessageMapping("/projet/{id}/action")
    public void handleAction(@DestinationVariable Long id, @Payload CollaborationMessage message, Authentication authentication) {
        String userEmail = authentication.getName();
        message.setSenderEmail(userEmail);

        switch (message.getType()) {
            case LOCK:
                if (lockService.acquireLock(message.getGranuleId(), userEmail)) {
                    messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                } else {
                    sendError(userEmail, "Lock denied for granule " + message.getGranuleId());
                }
                break;

            case UNLOCK:
                lockService.releaseLock(message.getGranuleId(), userEmail);
                messagingTemplate.convertAndSend("/topic/projet/" + id, message);
                // Sauvegarde asynchrone si c'est un UNLOCK_NODE (simulé ici par UNLOCK avec contenu)
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
                        sendError(userEmail, "You do not own the lock for this element");
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
        messagingTemplate.convertAndSendToUser(userEmail, "/topic/errors", error);
    }

    @Async
    public void saveContentAsync(Long exerciseId, String content) {
        log.info("Saving content for exercise {} asynchronously", exerciseId);
        exerciseRepository.findById(exerciseId.intValue()).ifPresent(exercise -> {
            exercise.setContent(content);
            exerciseRepository.save(exercise);
            log.info("Exercise {} saved successfully", exerciseId);
        });
    }

    @MessageExceptionHandler
    public void handleException(Exception exception, Authentication authentication) {
        log.error("WebSocket error: {}", exception.getMessage());
        if (authentication != null) {
            sendError(authentication.getName(), "Server error: " + exception.getMessage());
        }
    }
}

