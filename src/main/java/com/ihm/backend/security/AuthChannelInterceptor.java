package com.ihm.backend.security;

import com.ihm.backend.entity.Exercise;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.repository.jpa.EnrollmentRepository;
import com.ihm.backend.repository.jpa.ExerciseRepository;
import com.ihm.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ExerciseRepository exerciseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                authenticateConnection(accessor);
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                authorizeSubscription(accessor);
            }
        }
        return message;
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            String userEmail = jwtService.extractUsername(jwt);
            
            if (userEmail != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    accessor.setUser(authentication);
                    log.info("WebSocket user {} authenticated", userEmail);
                }
            }
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Authentication authentication = (Authentication) accessor.getUser();

        if (destination == null || authentication == null) {
            return;
        }

        if (destination.startsWith("/topic/projet/")) {
            try {
                String idPart = destination.substring("/topic/projet/".length());
                // Handle potential sub-paths like /topic/projet/1/presence
                if (idPart.contains("/")) {
                    idPart = idPart.substring(0, idPart.indexOf("/"));
                }
                Integer exerciseId = Integer.parseInt(idPart);
                
                if (!isAuthorized(exerciseId, authentication)) {
                    log.warn("User {} denied subscription to {}", authentication.getName(), destination);
                    throw new AccessDeniedException("Pas autorisé à s'abonner à ce projet");
                }
            } catch (NumberFormatException e) {
                log.error("Invalid exercise ID in destination: {}", destination);
            }
        }
    }

    private boolean isAuthorized(Integer exerciseId, Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            return false;
        }
        User user = (User) principal;
        String userEmail = user.getEmail();

        Optional<Exercise> exerciseOpt = exerciseRepository.findById(exerciseId);
        if (exerciseOpt.isEmpty()) return false;
        
        Exercise exercise = exerciseOpt.get();
        if (exercise.getCourse() == null) return false;

        // Est-ce l'auteur ?
        if (exercise.getCourse().getAuthor().getEmail().equals(userEmail)) {
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
}

