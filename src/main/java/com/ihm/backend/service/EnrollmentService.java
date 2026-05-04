package com.ihm.backend.service;

import com.ihm.backend.dto.EnrollmentDTO;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.Enrollment;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.repository.jpa.CourseRepository;
import com.ihm.backend.repository.jpa.EnrollmentRepository;
import com.ihm.backend.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Enrôle un utilisateur (étudiant ou enseignant) à un cours.
     * Les enseignants sont automatiquement approuvés.
     * Un enseignant ne peut pas s'enrôler à son propre cours.
     */
    @Transactional
    public EnrollmentDTO enrollUser(Integer courseId, UUID userId) throws Exception {
        log.info("Tentative d'enrôlement: userId={}, courseId={}", userId, courseId);

        // Vérifier que l'utilisateur existe et a un rôle autorisé
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (user.getRole() != UserRole.STUDENT && user.getRole() != UserRole.TEACHER) {
            throw new AccessDeniedException("Seuls les étudiants et les enseignants peuvent s'enrôler à des cours");
        }

        // Vérifier que le cours existe
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        // Autoriser l'enrôlement si le cours est publié OU si l'utilisateur est un enseignant (pour collaboration)
        if (course.getStatus() != CourseStatus.PUBLISHED && user.getRole() != UserRole.TEACHER) {
            throw new IllegalStateException("Ce cours n'est pas encore accessible");
        }

        // Un enseignant ne peut pas s'enrôler à son propre cours en tant qu'étudiant,
        // mais il peut être ajouté comme collaborateur (géré par une autre méthode normalement)
        if (user.getRole() == UserRole.TEACHER && course.getAuthor() != null
                && course.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("Vous êtes déjà l'auteur de ce cours");
        }

        // Vérifier s'il y a déjà un enrôlement
        Optional<Enrollment> existingEnrollment = enrollmentRepository.findByCourse_IdAndUser_Id(courseId, userId);
        if (existingEnrollment.isPresent()) {
            Enrollment enrollment = existingEnrollment.get();
            // Si l'utilisateur était invité, on valide simplement son enrôlement
            if (enrollment.getStatus() == com.ihm.backend.enums.EnrollmentStatus.INVITED) {
                enrollment.setStatus(com.ihm.backend.enums.EnrollmentStatus.APPROVED);
                return EnrollmentDTO.fromEntity(enrollmentRepository.save(enrollment));
            }
            throw new IllegalStateException("Vous êtes déjà inscrit à ce cours");
        }

        // Les enseignants sont automatiquement approuvés ; les étudiants restent PENDING
        com.ihm.backend.enums.EnrollmentStatus initialStatus =
                user.getRole() == UserRole.TEACHER
                        ? com.ihm.backend.enums.EnrollmentStatus.APPROVED
                        : com.ihm.backend.enums.EnrollmentStatus.PENDING;

        // Créer l'enrôlement
        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .progress(0.0)
                .completed(false)
                .status(initialStatus)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Enrôlement créé avec succès: id={}, statut={}", saved.getId(), initialStatus);

        // Notifier l'auteur du cours (seulement si c'est un étudiant qui s'enrôle)
        if (user.getRole() == UserRole.STUDENT) {
            notificationService.sendNewEnrollmentNotification(
                    course.getAuthor(),
                    user.getFullName(),
                    course.getTitle());
        }

        return EnrollmentDTO.fromEntity(saved);
    }

    /**
     * @deprecated Utilisez {@link #enrollUser(Integer, UUID)} à la place.
     */
    @Deprecated
    @Transactional
    public EnrollmentDTO enrollStudent(Integer courseId, UUID userId) throws Exception {
        return enrollUser(courseId, userId);
    }

    /**
     * Met à jour la progression d'un étudiant
     */
    @Transactional
    public EnrollmentDTO updateProgress(Long enrollmentId, Double progress) throws Exception {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("La progression doit être entre 0 et 100");
        }

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrôlement non trouvé"));

        enrollment.setProgress(progress);
        enrollment.setLastAccessed(LocalDateTime.now());

        // Marquer comme complété automatiquement si progression = 100%
        if (progress >= 100.0) {
            enrollment.setCompleted(true);
        }

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Progression mise à jour: enrollmentId={}, progress={}%", enrollmentId, progress);

        return EnrollmentDTO.fromEntity(saved);
    }

    /**
     * Marque un cours comme complété
     */
    @Transactional
    public EnrollmentDTO markAsCompleted(Long enrollmentId) throws Exception {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrôlement non trouvé"));

        enrollment.setCompleted(true);
        enrollment.setProgress(100.0);
        enrollment.setLastAccessed(LocalDateTime.now());

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Cours marqué comme complété: enrollmentId={}", enrollmentId);

        return EnrollmentDTO.fromEntity(saved);
    }

    /**
     * Récupère l'enrôlement d'un utilisateur pour un cours spécifique
     */
    public EnrollmentDTO getEnrollmentForUser(Integer courseId, UUID userId) {
        return enrollmentRepository.findByCourse_IdAndUser_Id(courseId, userId)
                .map(EnrollmentDTO::fromEntity)
                .orElse(null);
    }

    /**
     * Récupère tous les enrôlements d'un utilisateur
     */
    public List<EnrollmentDTO> getUserEnrollments(UUID userId) {
        return enrollmentRepository.findByUser_Id(userId)
                .stream()
                .map(EnrollmentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour la date du dernier accès
     */
    @Transactional
    public void updateLastAccessed(Long enrollmentId) {
        enrollmentRepository.findById(enrollmentId).ifPresent(enrollment -> {
            enrollment.setLastAccessed(LocalDateTime.now());
            enrollmentRepository.save(enrollment);
        });
    }

    /**
     * Valide ou rejette un enrôlement
     */
    @Transactional
    public EnrollmentDTO validateEnrollment(Long enrollmentId, com.ihm.backend.enums.EnrollmentStatus newStatus,
            UUID validatorId) throws Exception {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrôlement non trouvé"));

        // Vérifier que le validateur est le propriétaire ou un éditeur du cours
        boolean isAuthor = enrollment.getCourse().getAuthor().getId().equals(validatorId);
        boolean isEditor = enrollment.getCourse().getEditors() != null &&
                          enrollment.getCourse().getEditors().stream().anyMatch(u -> u.getId().equals(validatorId));

        if (!isAuthor && !isEditor) {
            log.warn("L'utilisateur {} a tenté de valider l'enrôlement {} sans avoir les droits", validatorId,
                    enrollmentId);
            throw new AccessDeniedException("Vous n'avez pas les droits pour valider cet enrôlement");
        }

        enrollment.setStatus(newStatus);
        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Statut de l'enrôlement {} mis à jour vers {}", enrollmentId, newStatus);

        // Notifier l'étudiant si accepté
        if (newStatus == com.ihm.backend.enums.EnrollmentStatus.APPROVED) {
            notificationService.sendEnrollmentAcceptedEmail(
                    enrollment.getUser(),
                    enrollment.getCourse().getTitle());
        }

        return EnrollmentDTO.fromEntity(saved);
    }

    /**
     * Récupère les enrôlements en attente pour les cours d'un enseignant
     */
    public List<EnrollmentDTO> getPendingEnrollmentsForTeacher(UUID teacherId) {
        return enrollmentRepository
                .findByCourse_Author_IdAndStatus(teacherId, com.ihm.backend.enums.EnrollmentStatus.PENDING)
                .stream()
                .map(EnrollmentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Désenrôle un étudiant d'un cours
     */
    @Transactional
    public void unenroll(Long enrollmentId, UUID userId) throws Exception {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrôlement non trouvé"));

        if (!enrollment.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres enrôlements");
        }

        enrollmentRepository.delete(enrollment);
        log.info("L'utilisateur {} s'est désenrôlé du cours {}", userId, enrollment.getCourse().getId());
    }

    /**
     * Annule un enrôlement en attente
     */
    @Transactional
    public void cancelPendingEnrollment(Long enrollmentId, UUID userId) throws Exception {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrôlement non trouvé"));

        if (!enrollment.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez annuler que vos propres enrôlements");
        }

        if (enrollment.getStatus() != com.ihm.backend.enums.EnrollmentStatus.PENDING) {
            throw new IllegalStateException("Seuls les enrôlements en attente peuvent être annulés");
        }

        enrollmentRepository.delete(enrollment);
        log.info("L'utilisateur {} a annulé sa demande d'enrôlement en attente pour le cours {}", userId,
                enrollment.getCourse().getId());
    }

    /**
     * Invite un utilisateur à un cours par email
     */
    @Transactional
    public EnrollmentDTO inviteUser(Integer courseId, String email, UUID inviterId) throws Exception {
        log.info("Invitation de l'utilisateur {} au cours {} par {}", email, courseId, inviterId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        // Seul l'auteur, un éditeur ou un admin peut inviter
        boolean isAuthor = course.getAuthor().getId().equals(inviterId);
        boolean isEditor = course.getEditors() != null &&
                          course.getEditors().stream().anyMatch(u -> u.getId().equals(inviterId));

        if (!isAuthor && !isEditor) {
            throw new AccessDeniedException("Vous n'avez pas les droits pour inviter des collaborateurs à ce cours");
        }

        User invitedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'email: " + email));

        // Vérifier s'il est déjà enrôlé
        if (enrollmentRepository.existsByCourse_IdAndUser_Id(courseId, invitedUser.getId())) {
            throw new IllegalStateException("Cet utilisateur est déjà associé à ce cours");
        }

        Enrollment enrollment = Enrollment.builder()
                .user(invitedUser)
                .course(course)
                .progress(0.0)
                .completed(false)
                .status(com.ihm.backend.enums.EnrollmentStatus.INVITED)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Invitation créée: id={}", saved.getId());

        // Notifier l'invité
        notificationService.sendEnrollmentAcceptedEmail(invitedUser, "Invitation au cours: " + course.getTitle());

        return EnrollmentDTO.fromEntity(saved);
    }
}
