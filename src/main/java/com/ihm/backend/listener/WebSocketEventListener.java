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

import com.ihm.backend.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private static final String PREFIX = "/topic/projet/";
    private static final String SESSION_KEY = "subscribedProjects";

    private final SimpMessagingTemplate messagingTemplate;
    private final LockService lockService;

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();

        if (destination == null || !destination.startsWith(PREFIX)) return;

        // Ignorer les sous-canaux comme /topic/projet/19/presence — seul /topic/projet/19 déclenche JOIN
        String afterPrefix = destination.substring(PREFIX.length());
        if (afterPrefix.isEmpty() || afterPrefix.contains("/")) return;

        String projectId = afterPrefix;

        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        if (sessionAttrs != null) {
            @SuppressWarnings("unchecked")
            Set<String> projects = (Set<String>) sessionAttrs.get(SESSION_KEY);
            if (projects == null) {
                projects = ConcurrentHashMap.newKeySet();
                sessionAttrs.put(SESSION_KEY, projects);
            }
            projects.add(projectId);
        }

        Principal user = headerAccessor.getUser();
        if (user != null) {
            String email = user.getName();
            String fullName = resolveFullName(user);
            log.info("User {} joined project {}", email, projectId);
            PresenceMessage presenceMessage = PresenceMessage.builder()
                    .type(PresenceMessage.PresenceType.JOIN)
                    .userEmail(email)
                    .userName(fullName)
                    .build();
            messagingTemplate.convertAndSend(PREFIX + projectId + "/presence", presenceMessage);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user == null) return;

        String userEmail = user.getName();
        String fullName = resolveFullName(user);
        log.info("User Disconnected: {}", userEmail);
        lockService.releaseAllLocksForUser(userEmail);

        PresenceMessage presenceMessage = PresenceMessage.builder()
                .type(PresenceMessage.PresenceType.LEAVE)
                .userEmail(userEmail)
                .userName(fullName)
                .build();

        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        if (sessionAttrs != null) {
            @SuppressWarnings("unchecked")
            Set<String> projects = (Set<String>) sessionAttrs.get(SESSION_KEY);
            if (projects != null && !projects.isEmpty()) {
                // Notifier chaque projet auquel l'utilisateur était abonné
                projects.forEach(projectId ->
                    messagingTemplate.convertAndSend(PREFIX + projectId + "/presence", presenceMessage)
                );
                return;
            }
        }

        messagingTemplate.convertAndSend("/topic/presence", presenceMessage);
    }

    private String resolveFullName(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof User u) {
            return u.getFullName();
        }
        return principal.getName();
    }
}

