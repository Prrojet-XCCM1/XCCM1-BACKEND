package com.ihm.backend.service;

import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.CourseInvitation;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.InvitationStatus;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.repository.jpa.CourseInvitationRepository;
import com.ihm.backend.repository.jpa.CourseRepository;
import com.ihm.backend.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseInvitationService {

    private final CourseInvitationRepository invitationRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public CourseInvitation inviteEditor(Integer courseId, String emailOrName, UUID inviterId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        if (!course.getAuthor().getId().equals(inviterId)) {
            throw new RuntimeException("Seul l'auteur du cours peut inviter des éditeurs");
        }

        // Search for user
        User invitedUser = userRepository.findByEmail(emailOrName)
                .orElseGet(() -> {
                    List<User> users = userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(emailOrName, emailOrName);
                    return users.isEmpty() ? null : users.get(0); // For now, take first match or null
                });

        String targetEmail = invitedUser != null ? invitedUser.getEmail() : emailOrName;
        String token = UUID.randomUUID().toString();
        
        CourseInvitation invitation = CourseInvitation.builder()
                .course(course)
                .inviter(course.getAuthor())
                .email(targetEmail)
                .token(token)
                .status(InvitationStatus.PENDING)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        CourseInvitation saved = invitationRepository.save(invitation);
        
        // Send email
        notificationService.sendEditorInvitationEmail(course.getAuthor(), course, targetEmail, token);
        
        // Send push notification if user exists
        if (invitedUser != null) {
            notificationService.sendRealTimeNotification(
                invitedUser.getId(),
                "Vous avez été invité à devenir éditeur pour le cours: " + course.getTitle(),
                "COURSE_INVITATION"
            );
        }

        return saved;
    }

    @Transactional
    public void acceptInvitation(String token, User currentUser) {
        CourseInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation invalide"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Cettte invitation a déjà été traitée");
        }

        if (invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalStateException("Cette invitation a expiré");
        }

        // Verify that the current user is the target of the invitation
        if (!invitation.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            throw new RuntimeException("Cette invitation n'est pas destinée à votre compte");
        }

        Course course = invitation.getCourse();
        if (!course.getEditors().contains(currentUser)) {
            course.getEditors().add(currentUser);
            courseRepository.save(course);
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
        
        log.info("L'utilisateur {} est maintenant éditeur du cours {}", currentUser.getEmail(), course.getId());
    }

    public List<User> searchUsers(String query) {
        if (query.contains("@")) {
            return userRepository.findByEmail(query).map(List::of).orElse(List.of());
        }
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query);
    }
}
