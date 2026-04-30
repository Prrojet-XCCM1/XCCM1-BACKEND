package com.ihm.backend.listener;

import com.ihm.backend.dto.websocket.PresenceMessage;
import com.ihm.backend.service.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final LockService lockService;

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();

        if (destination != null && destination.startsWith("/topic/projet/")) {
            String projectId = extractProjectId(destination);
            if (projectId != null) {
                // Stocker le projet ID actuel dans les attributs de session pour le gérer à la déconnexion
                if (headerAccessor.getSessionAttributes() != null) {
                    headerAccessor.getSessionAttributes().put("currentProjectId", projectId);
                }

                Principal user = headerAccessor.getUser();
                if (user != null) {
                    log.info("User {} joined project {}", user.getName(), projectId);
                    PresenceMessage presenceMessage = PresenceMessage.builder()
                            .type(PresenceMessage.PresenceType.JOIN)
                            .userEmail(user.getName())
                            .build();
                    messagingTemplate.convertAndSend("/topic/projet/" + projectId + "/presence", presenceMessage);
                }
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String userEmail = user.getName();
            log.info("User Disconnected: {}", userEmail);

            // Libérer tous les verrous de l'utilisateur
            lockService.releaseAllLocksForUser(userEmail);

            // Récupérer le projet ID depuis la session pour notifier le départ
            String projectId = null;
            if (headerAccessor.getSessionAttributes() != null) {
                projectId = (String) headerAccessor.getSessionAttributes().get("currentProjectId");
            }

            PresenceMessage presenceMessage = PresenceMessage.builder()
                    .type(PresenceMessage.PresenceType.LEAVE)
                    .userEmail(userEmail)
                    .build();

            if (projectId != null) {
                messagingTemplate.convertAndSend("/topic/projet/" + projectId + "/presence", presenceMessage);
            } else {
                // Fallback sur le canal global si on n'a pas pu identifier le projet
                messagingTemplate.convertAndSend("/topic/presence", presenceMessage);
            }
        }
    }

    private String extractProjectId(String destination) {
        try {
            String idPart = destination.substring("/topic/projet/".length());
            if (idPart.contains("/")) {
                idPart = idPart.substring(0, idPart.indexOf("/"));
            }
            return idPart;
        } catch (Exception e) {
            return null;
        }
    }
}

