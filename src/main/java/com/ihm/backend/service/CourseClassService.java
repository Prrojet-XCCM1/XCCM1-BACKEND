package com.ihm.backend.service;

import com.ihm.backend.dto.ClassEnrollmentDTO;
import com.ihm.backend.dto.request.CourseClassCreateRequest;
import com.ihm.backend.dto.request.CourseClassUpdateRequest;
import com.ihm.backend.dto.response.CourseClassResponse;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.CourseClass;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.ClassStatus;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.repository.ClassEnrollmentRepository;
import com.ihm.backend.repository.CourseClassRepository;
import com.ihm.backend.repository.CourseRepository;
import com.ihm.backend.repository.EnrollmentRepository;
import com.ihm.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseClassService {

    private final CourseClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final EnrollmentRepository courseEnrollmentRepository;

    // ─── CRUD ────────────────────────────────────────────────────────────────

    /**
     * Crée une nouvelle classe de cours pour un enseignant
     */
    @Transactional
    public CourseClassResponse createClass(CourseClassCreateRequest request, UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant non trouvé"));

        CourseClass entity = CourseClass.builder()
                .name(request.getName())
                .description(request.getDescription())
                .theme(request.getTheme())
                .coverImage(request.getCoverImage())
                .maxStudents(request.getMaxStudents() != null ? request.getMaxStudents() : 0)
                .status(ClassStatus.OPEN)
                .teacher(teacher)
                .build();

        CourseClass saved = classRepository.save(entity);
        log.info("Classe de cours créée: id={}, name={}, teacher={}", saved.getId(), saved.getName(), teacherId);
        return buildResponse(saved, null);
    }

    /**
     * Récupère les classes d'un enseignant
     */
    @Transactional(readOnly = true)
    public List<CourseClassResponse> getMyClasses(UUID teacherId) {
        return classRepository.findByTeacher_Id(teacherId).stream()
                .map(c -> buildResponse(c, null))
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les classes OPEN (pour les étudiants qui veulent s'inscrire)
     * Enrichit avec l'inscription de l'étudiant courant si fourni
     */
    @Transactional(readOnly = true)
    public List<CourseClassResponse> getAllOpenClasses(UUID studentId) {
        return classRepository.findByStatus(ClassStatus.OPEN).stream()
                .map(c -> buildEnrichedResponse(c, studentId))
                .collect(Collectors.toList());
    }

    /**
     * Récupère une classe par ID
     * Enrichit avec l'inscription de l'utilisateur courant si fourni
     */
    @Transactional(readOnly = true)
    public CourseClassResponse getClassById(Long classId, UUID userId) {
        CourseClass entity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe non trouvée: " + classId));
        return buildEnrichedResponse(entity, userId);
    }

    /**
     * Met à jour une classe (vérification ownership)
     */
    @Transactional
    public CourseClassResponse updateClass(Long classId, CourseClassUpdateRequest request, UUID teacherId) throws AccessDeniedException {
        CourseClass entity = getAndValidateOwnership(classId, teacherId);

        if (request.getName() != null)        entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getTheme() != null)        entity.setTheme(request.getTheme());
        if (request.getCoverImage() != null)  entity.setCoverImage(request.getCoverImage());
        if (request.getMaxStudents() != null)  entity.setMaxStudents(request.getMaxStudents());

        CourseClass saved = classRepository.save(entity);
        log.info("Classe mise à jour: id={}", classId);
        return buildResponse(saved, null);
    }

    /**
     * Supprime une classe (vérification ownership)
     */
    @Transactional
    public void deleteClass(Long classId, UUID teacherId) throws AccessDeniedException {
        CourseClass entity = getAndValidateOwnership(classId, teacherId);

        // Détacher les cours de cette classe avant suppression
        if (entity.getCourses() != null) {
            entity.getCourses().forEach(course -> course.setCourseClass(null));
            courseRepository.saveAll(entity.getCourses());
        }

        classRepository.delete(entity);
        log.info("Classe supprimée: id={}", classId);
    }

    /**
     * Change le statut d'une classe (OPEN, CLOSED, ARCHIVED)
     */
    @Transactional
    public CourseClassResponse changeStatus(Long classId, ClassStatus newStatus, UUID teacherId) throws AccessDeniedException {
        CourseClass entity = getAndValidateOwnership(classId, teacherId);
        entity.setStatus(newStatus);
        CourseClass saved = classRepository.save(entity);
        log.info("Statut de la classe {} changé vers {}", classId, newStatus);
        return buildResponse(saved, null);
    }

    // ─── GESTION DES COURS ───────────────────────────────────────────────────

    /**
     * Ajoute un cours à une classe (le cours doit appartenir au même enseignant)
     */
    @Transactional
    public CourseClassResponse addCourseToClass(Long classId, Integer courseId, UUID teacherId) throws AccessDeniedException {
        CourseClass entity = getAndValidateOwnership(classId, teacherId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé: " + courseId));

        if (!course.getAuthor().getId().equals(teacherId)) {
            throw new AccessDeniedException("Vous ne pouvez ajouter que vos propres cours à une classe");
        }

        if (course.getCourseClass() != null && !course.getCourseClass().getId().equals(classId)) {
            throw new IllegalStateException("Ce cours appartient déjà à une autre classe");
        }

        course.setCourseClass(entity);
        courseRepository.save(course);
        log.info("Cours {} ajouté à la classe {}", courseId, classId);

        // Recharger pour avoir la liste à jour
        CourseClass reloaded = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe non trouvée"));
        return buildResponse(reloaded, null);
    }

    /**
     * Retire un cours d'une classe
     */
    @Transactional
    public CourseClassResponse removeCourseFromClass(Long classId, Integer courseId, UUID teacherId) throws AccessDeniedException {
        getAndValidateOwnership(classId, teacherId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé: " + courseId));

        if (course.getCourseClass() == null || !course.getCourseClass().getId().equals(classId)) {
            throw new IllegalStateException("Ce cours n'appartient pas à cette classe");
        }

        course.setCourseClass(null);
        courseRepository.save(course);
        log.info("Cours {} retiré de la classe {}", courseId, classId);

        CourseClass reloaded = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe non trouvée"));
        return buildResponse(reloaded, null);
    }

    // ─── IMAGE DE COUVERTURE ─────────────────────────────────────────────────

    /**
     * Upload une image de couverture pour la classe
     */
    @Transactional
    public CourseClassResponse uploadCoverImage(Long classId, MultipartFile image, UUID teacherId)
            throws AccessDeniedException, IOException {
        CourseClass entity = getAndValidateOwnership(classId, teacherId);

        Path uploadDir = Paths.get("uploads/classes");
        Files.createDirectories(uploadDir);
        String fileName = "class_" + classId + "_" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
        Path filePath = uploadDir.resolve(fileName);
        Files.write(filePath, image.getBytes());

        entity.setCoverImage("/uploads/classes/" + fileName);
        CourseClass saved = classRepository.save(entity);
        return buildResponse(saved, null);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    /**
     * Vérifie que l'enseignant est bien le propriétaire de la classe
     */
    private CourseClass getAndValidateOwnership(Long classId, UUID teacherId) throws AccessDeniedException {
        CourseClass entity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe non trouvée: " + classId));

        if (!entity.getTeacher().getId().equals(teacherId)) {
            throw new AccessDeniedException("Vous n'êtes pas le propriétaire de cette classe");
        }
        return entity;
    }

    /**
     * Construit la réponse avec les compteurs d'inscrits.
     * participantCount = somme totale des participants dans tous les cours de la classe (sans distinction).
     */
    private CourseClassResponse buildResponse(CourseClass entity, ClassEnrollmentDTO myEnrollment) {
        // Participants = ensemble de toutes les inscriptions pour chaque cours de cette classe
        long participantCount = courseEnrollmentRepository.countTotalParticipantsByClassId(entity.getId());
        // Demandes en attente (inscription à la classe)
        long pendingCount = enrollmentRepository.countByCourseClass_IdAndStatus(entity.getId(), EnrollmentStatus.PENDING);

        CourseClassResponse response = CourseClassResponse.fromEntity(entity, participantCount, pendingCount);
        response.setMyEnrollment(myEnrollment);
        return response;
    }

    /**
     * Construit la réponse enrichie avec l'enrollment de l'utilisateur courant
     */
    private CourseClassResponse buildEnrichedResponse(CourseClass entity, UUID userId) {
        ClassEnrollmentDTO myEnrollment = null;
        if (userId != null) {
            myEnrollment = enrollmentRepository
                    .findByCourseClass_IdAndStudent_Id(entity.getId(), userId)
                    .map(ClassEnrollmentDTO::fromEntity)
                    .orElse(null);
        }
        return buildResponse(entity, myEnrollment);
    }
}
