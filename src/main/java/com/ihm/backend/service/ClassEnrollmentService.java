package com.ihm.backend.service;

import com.ihm.backend.dto.ClassEnrollmentDTO;
import com.ihm.backend.entity.ClassEnrollment;
import com.ihm.backend.entity.CourseClass;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.ClassStatus;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.repository.ClassEnrollmentRepository;
import com.ihm.backend.repository.CourseClassRepository;
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
public class ClassEnrollmentService {

    private final ClassEnrollmentRepository enrollmentRepository;
    private final CourseClassRepository classRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ─── INSCRIPTIONS ÉTUDIANTS ──────────────────────────────────────────────

    /**
     * Inscrit un étudiant à une classe de cours.
     * Vérifie: classe OPEN, pas de doublon, max non atteint.
     */
    @Transactional
    public ClassEnrollmentDTO enrollInClass(Long classId, UUID studentId) throws Exception {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant non trouvé"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new AccessDeniedException("Seuls les étudiants peuvent s'inscrire à une classe");
        }

        CourseClass courseClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe non trouvée: " + classId));

        if (courseClass.getStatus() != ClassStatus.OPEN) {
            throw new IllegalStateException("Cette classe n'accepte plus d'inscriptions (statut: " + courseClass.getStatus() + ")");
        }

        if (enrollmentRepository.existsByCourseClass_IdAndStudent_Id(classId, studentId)) {
            throw new IllegalStateException("Vous êtes déjà inscrit à cette classe");
        }

        // Vérifier le nombre maximum d'étudiants
        int maxStudents = courseClass.getMaxStudents() != null ? courseClass.getMaxStudents() : 0;
        if (maxStudents > 0) {
            long approvedCount = enrollmentRepository.countByCourseClass_IdAndStatus(classId, EnrollmentStatus.APPROVED);
            if (approvedCount >= maxStudents) {
                throw new IllegalStateException("La classe est complète (maximum " + maxStudents + " étudiants)");
            }
        }

        ClassEnrollment enrollment = ClassEnrollment.builder()
                .student(student)
                .courseClass(courseClass)
                .status(EnrollmentStatus.PENDING)
                .build();

        ClassEnrollment saved = enrollmentRepository.save(enrollment);
        log.info("Inscription à la classe créée: studentId={}, classId={}, enrollmentId={}", studentId, classId, saved.getId());

        // Notifier l'enseignant
        notificationService.sendNewEnrollmentNotification(
                courseClass.getTeacher(),
                student.getFullName(),
                courseClass.getName());

        return ClassEnrollmentDTO.fromEntity(saved);
    }

    /**
     * Désinscrit un étudiant d'une classe (supprime l'inscription peu importe son statut)
     */
    @Transactional
    public void unenrollFromClass(Long enrollmentId, UUID studentId) throws AccessDeniedException {
        ClassEnrollment enrollment = getEnrollmentAndValidateStudent(enrollmentId, studentId);
        enrollmentRepository.delete(enrollment);
        log.info("Étudiant {} désinscrit de la classe {} (enrollment {})",
                studentId, enrollment.getCourseClass().getId(), enrollmentId);
    }

    /**
     * Annule une demande d'inscription en attente (PENDING uniquement)
     */
    @Transactional
    public void cancelPendingEnrollment(Long enrollmentId, UUID studentId) throws AccessDeniedException {
        ClassEnrollment enrollment = getEnrollmentAndValidateStudent(enrollmentId, studentId);

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new IllegalStateException("Seules les inscriptions en attente (PENDING) peuvent être annulées");
        }

        enrollmentRepository.delete(enrollment);
        log.info("Demande d'inscription en attente annulée: enrollmentId={}, studentId={}", enrollmentId, studentId);
    }

    /**
     * Récupère toutes les inscriptions d'un étudiant
     */
    public List<ClassEnrollmentDTO> getMyEnrollments(UUID studentId) {
        return enrollmentRepository.findByStudent_Id(studentId).stream()
                .map(ClassEnrollmentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Récupère l'inscription d'un étudiant à une classe spécifique
     */
    public ClassEnrollmentDTO getEnrollmentForClass(Long classId, UUID studentId) {
        return enrollmentRepository.findByCourseClass_IdAndStudent_Id(classId, studentId)
                .map(ClassEnrollmentDTO::fromEntity)
                .orElse(null);
    }

    // ─── GESTION ENSEIGNANT ──────────────────────────────────────────────────

    /**
     * Valide ou rejette une inscription.
     * Seul l'enseignant propriétaire de la classe peut le faire.
     */
    @Transactional
    public ClassEnrollmentDTO validateEnrollment(Long enrollmentId, EnrollmentStatus newStatus, UUID teacherId)
            throws AccessDeniedException {
        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription non trouvée: " + enrollmentId));

        // Vérifier que c'est bien l'enseignant de la classe
        if (!enrollment.getCourseClass().getTeacher().getId().equals(teacherId)) {
            throw new AccessDeniedException("Vous n'êtes pas l'enseignant de cette classe");
        }

        if (newStatus != EnrollmentStatus.APPROVED && newStatus != EnrollmentStatus.REJECTED) {
            throw new IllegalArgumentException("Le statut doit être APPROVED ou REJECTED");
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant non trouvé"));

        enrollment.setStatus(newStatus);
        enrollment.setValidatedAt(LocalDateTime.now());
        enrollment.setValidatedBy(teacher);

        ClassEnrollment saved = enrollmentRepository.save(enrollment);
        log.info("Inscription {} mise à jour vers {} par l'enseignant {}", enrollmentId, newStatus, teacherId);

        // Notifier l'étudiant si accepté
        if (newStatus == EnrollmentStatus.APPROVED) {
            notificationService.sendEnrollmentAcceptedEmail(
                    enrollment.getStudent(),
                    enrollment.getCourseClass().getName());
        }

        return ClassEnrollmentDTO.fromEntity(saved);
    }

    /**
     * Récupère tous les inscrits d'une classe (enseignant uniquement)
     */
    public List<ClassEnrollmentDTO> getClassEnrollments(Long classId, UUID teacherId) throws AccessDeniedException {
        CourseClass courseClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe non trouvée: " + classId));

        if (!courseClass.getTeacher().getId().equals(teacherId)) {
            throw new AccessDeniedException("Vous n'êtes pas l'enseignant de cette classe");
        }

        return enrollmentRepository.findByCourseClass_Id(classId).stream()
                .map(ClassEnrollmentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les demandes PENDING pour les classes d'un enseignant
     */
    public List<ClassEnrollmentDTO> getPendingForTeacher(UUID teacherId) {
        return enrollmentRepository
                .findByCourseClass_Teacher_IdAndStatus(teacherId, EnrollmentStatus.PENDING).stream()
                .map(ClassEnrollmentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private ClassEnrollment getEnrollmentAndValidateStudent(Long enrollmentId, UUID studentId)
            throws AccessDeniedException {
        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription non trouvée: " + enrollmentId));

        if (!enrollment.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres inscriptions");
        }
        return enrollment;
    }
}
