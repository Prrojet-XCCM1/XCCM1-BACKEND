package com.ihm.backend.service;

import com.ihm.backend.dto.EnrollmentDTO;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.Enrollment;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.repository.CourseRepository;
import com.ihm.backend.repository.EnrollmentRepository;
import com.ihm.backend.repository.UserRepository;
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
     * Enrôle un étudiant à un cours
     */
    @Transactional
    public EnrollmentDTO enrollStudent(Integer courseId, UUID userId) throws Exception {
        log.info("Tentative d'enrôlement: userId={}, courseId={}", userId, courseId);

        // Vérifier que l'utilisateur existe et est un étudiant
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (user.getRole() != UserRole.STUDENT) {
            throw new AccessDeniedException("Seuls les étudiants peuvent s'enrôler à des cours");
        }

        // Vérifier que le cours existe et est publié
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalStateException("Ce cours n'est pas encore publié");
        }

        // Vérifier qu'il n'y a pas de doublon
        if (enrollmentRepository.existsByCourse_IdAndUser_Id(courseId, userId)) {
            throw new IllegalStateException("Vous êtes déjà enrôlé à ce cours");
        }

        // Créer l'enrôlement
        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .progress(0.0)
                .completed(false)
                .status(com.ihm.backend.enums.EnrollmentStatus.PENDING) // Toujours PENDING au début
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Enrôlement créé avec succès: id={}", saved.getId());

        // Notifier l'enseignant
        notificationService.sendNewEnrollmentNotification(
                course.getAuthor(),
                user.getFullName(),
                course.getTitle());

        return EnrollmentDTO.fromEntity(saved);
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

        // Vérifier que le validateur est le propriétaire du cours
        if (!enrollment.getCourse().getAuthor().getId().equals(validatorId)) {
            // TODO: Ajouter vérification ADMIN si nécessaire
            log.warn("L'utilisateur {} a tenté de valider l'enrôlement {} sans être l'auteur du cours", validatorId,
                    enrollmentId);
            throw new AccessDeniedException("Vous n'êtes pas l'auteur de ce cours");
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
}
