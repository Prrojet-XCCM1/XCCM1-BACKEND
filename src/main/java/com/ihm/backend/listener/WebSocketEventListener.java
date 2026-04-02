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

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final LockService lockService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String userEmail = user.getName();
            log.info("User Disconnected: {}", userEmail);

            // Libérer tous les verrous de l'utilisateur
            lockService.releaseAllLocksForUser(userEmail);

            // Diffuser le message de départ
            PresenceMessage presenceMessage = PresenceMessage.builder()
                    .type(PresenceMessage.PresenceType.LEAVE)
                    .userEmail(userEmail)
                    .build();

            messagingTemplate.convertAndSend("/topic/presence", presenceMessage);
        }
    }
}
