package com.ihm.backend.service;

import java.nio.file.AccessDeniedException;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ihm.backend.dto.response.CourseResponse;
import com.ihm.backend.dto.response.EnrichedCourseResponse;
import com.ihm.backend.dto.request.CourseCreateRequest;
import com.ihm.backend.dto.request.CourseUpdateRequest;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.mappers.CourseMapper;
import com.ihm.backend.repository.jpa.CourseRepository;
import com.ihm.backend.repository.jpa.EnrollmentRepository;
import com.ihm.backend.repository.jpa.UserRepository;

import java.util.UUID;
import com.ihm.backend.entity.*;
import com.ihm.backend.enums.CourseStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CourseService {
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private com.ihm.backend.repository.elasticsearch.CourseSearchRepository courseSearchRepository;
    @Autowired
    private LLMIndexingService llmIndexingService;

    public List<Course> searchCourses(String query) {
        try {
            // Tentative de recherche via Elasticsearch
            return (List<Course>) courseSearchRepository.findAll();
        } catch (Exception e) {
            log.warn("Elasticsearch est indisponible pour la recherche de cours, repli sur JPA: {}", e.getMessage());
            return courseRepository.searchPublishedCourses(query);
        }
    }

    // create a course
    @Transactional
    public CourseResponse createCourse(CourseCreateRequest dto, UUID authorId) throws Exception {
        Course course = courseMapper.toEntity(dto);
        User author = userRepository.findById(authorId).orElseThrow(() -> new Exception("Teacher does not exists"));
        course.setAuthor(author);
        course = courseRepository.save(course);
        
        try {
            courseSearchRepository.save(course);
        } catch (Exception e) {
            log.error("Impossible de synchroniser le cours avec Elasticsearch: {}", e.getMessage());
        }
        
        return courseMapper.toResponse(course);
    }
    // get all courses for a particular author

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCoursesForTeacher(UUID authorId) throws Exception {
        User author = userRepository.findById(authorId).orElseThrow(() -> new Exception("Teacher does not exists"));
        return courseMapper.toResponse(courseRepository.findByAuthor(author));
    }

    // update course
    @Transactional
    public CourseResponse updateCourse(Integer courseId, CourseUpdateRequest request) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new Exception("Course does not exist"));

        courseMapper.updateEntity(request, course);
        course = courseRepository.save(course);
        
        try {
            courseSearchRepository.save(course);
        } catch (Exception e) {
            log.error("Impossible de synchroniser la mise à jour du cours avec Elasticsearch: {}", e.getMessage());
        }
        
        return courseMapper.toResponse(course);
    }

    // get all courses
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseMapper.toResponse(courseRepository.findByStatus(CourseStatus.PUBLISHED));
    }

    // delete course
    @Transactional
    public void deleteCourse(Integer courseId) throws Exception {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new Exception("Course does not exist"));
        courseRepository.delete(course);
        
        try {
            courseSearchRepository.delete(course);
        } catch (Exception e) {
            log.error("Impossible de supprimer le cours d'Elasticsearch: {}", e.getMessage());
        }
    }

    // changeState of Course
    @Transactional
    public CourseResponse changeCourseStatus(CourseStatus courseStatus, Integer courseId) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new Exception("Course does not exist"));
        course.setStatus(courseStatus);
        courseRepository.save(course);
        
        try {
            courseSearchRepository.save(course);
        } catch (Exception e) {
            log.error("Impossible de synchroniser le changement de statut du cours avec Elasticsearch: {}", e.getMessage());
        }

        if (courseStatus == CourseStatus.PUBLISHED) {
            notificationService.sendCoursePublishedEmail(course.getAuthor(), course.getTitle());
            llmIndexingService.indexCourse(course);
        }

        return courseMapper.toResponse(course);
    }

    public List<CourseResponse> getCoursesByStatusForAuthor(UUID authorId, CourseStatus courseStatus)
            throws Exception {
        List<Course> courses = courseRepository.findByStatus(courseStatus);
        List<Course> result = courses.stream().filter(course -> course.getAuthor().getId().equals(authorId))
                .collect(Collectors.toList());

        return courseMapper.toResponse(result);
    }

    public CourseResponse uploadCoverImage(Integer courseId, MultipartFile image) throws Exception {

        Course course = courseRepository.findById(courseId).orElseThrow(() -> new Exception());

        String fileName = image.getOriginalFilename();
        Path uploadDir = Paths.get("uploads");
        Files.createDirectories(uploadDir);

        Path filePath = uploadDir.resolve(fileName);
        Files.write(filePath, image.getBytes());

        // Save path accessible from frontend
        String urlPath = "/uploads/" + fileName;
        course.setCoverImage(urlPath);
        courseRepository.save(course);
        return courseMapper.toResponse(course);

    }

    /**
     * Valide que l'enseignant est propriétaire du cours
     */
    @Transactional(readOnly = true)
    public void validateOwnership(Integer courseId, UUID teacherId) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        boolean isAuthor = course.getAuthor().getId().equals(teacherId);
        boolean isEditor = course.getEditors() != null && 
                          course.getEditors().stream().anyMatch(u -> u.getId().equals(teacherId));

        if (!isAuthor && !isEditor) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres cours ou ceux dont vous êtes collaborateur");
        }
    }

    /**
     * Récupère tous les cours publiés enrichis avec l'enrôlement de l'utilisateur
     * si applicable
     */
    @Transactional(readOnly = true)
    public List<EnrichedCourseResponse> getEnrichedCourses(UUID userId) {
        List<Course> publishedCourses = courseRepository.findByStatus(CourseStatus.PUBLISHED);

        return publishedCourses.stream()
                .map(course -> {
                    Enrollment enrollment = null;
                    if (userId != null) {
                        enrollment = enrollmentRepository.findByCourse_IdAndUser_Id(
                                course.getId(), userId).orElse(null);
                    }
                    return EnrichedCourseResponse.fromCourse(course, enrollment);
                })
                .collect(Collectors.toList());
    }

    /**
     * Récupère un cours enrichi avec l'enrôlement de l'utilisateur si applicable
     */
    @Transactional(readOnly = true)
    public EnrichedCourseResponse getEnrichedCourse(Integer courseId, UUID userId) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        Enrollment enrollment = null;
        if (userId != null) {
            enrollment = enrollmentRepository.findByCourse_IdAndUser_Id(
                    courseId, userId).orElse(null);
        }

        return EnrichedCourseResponse.fromCourse(course, enrollment);
    }


    public CourseResponse incrementViewCount(Integer courseId) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));
        course.setViewCount(course.getViewCount() + 1);
        course = courseRepository.save(course);
        return courseMapper.toResponse(course);
    }

    public CourseResponse incrementLikeCount(Integer courseId) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));
        course.setLikeCount(course.getLikeCount() + 1);
        course = courseRepository.save(course);
        return courseMapper.toResponse(course);
    }

    public CourseResponse decrementLikeCount(Integer courseId) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));
        course.setLikeCount(course.getLikeCount() - 1);
        course = courseRepository.save(course);
        return courseMapper.toResponse(course);
    }


    public CourseResponse incrementDownloadCount(Integer courseId) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));
        course.setDownloadCount(course.getDownloadCount() + 1);
        course = courseRepository.save(course);
        return courseMapper.toResponse(course);
    }

    public List<Map<String, Object>> getRecommendations(String title, String description) {
        return llmIndexingService.recommendCourses(title, description);
    }
}
