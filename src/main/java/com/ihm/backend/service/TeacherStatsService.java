package com.ihm.backend.service;

import com.ihm.backend.dto.response.*;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.Exercise;
import com.ihm.backend.entity.StudentExercise;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherStatsService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExerciseRepository exerciseRepository;
    private final StudentExerciseRepository studentExerciseRepository;

    @Transactional(readOnly = true)
    public ApiResponse<TeacherCourseStatsResponse> getCourseStatistics(UUID teacherId, Integer courseId) {
        log.info("Récupération des statistiques du cours {} pour l'enseignant {}", courseId, teacherId);

        // Vérifier que le cours existe et appartient à l'enseignant
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé avec l'ID: " + courseId));

        if (!course.getAuthor().getId().equals(teacherId)) {
            throw new AccessDeniedException("Vous n'avez pas accès aux statistiques de ce cours");
        }

        // Calculer les statistiques d'enrollment
        long totalEnrolled = enrollmentRepository.countByCourse_Id(courseId);
        long pendingEnrolled = enrollmentRepository.countByCourse_IdAndStatus(courseId, com.ihm.backend.enums.EnrollmentStatus.PENDING);
        long approvedEnrolled = enrollmentRepository.countByCourse_IdAndStatus(courseId, com.ihm.backend.enums.EnrollmentStatus.APPROVED);
        long rejectedEnrolled = enrollmentRepository.countByCourse_IdAndStatus(courseId, com.ihm.backend.enums.EnrollmentStatus.REJECTED);
        
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeStudents = enrollmentRepository.countByCourse_IdAndLastAccessedAfter(courseId, sevenDaysAgo);
        
        Double participationRate = totalEnrolled > 0 
            ? (activeStudents * 100.0 / totalEnrolled) 
            : 0.0;

        // Calculer la progression moyenne
        List<Object[]> enrollmentStats = enrollmentRepository.findEnrollmentStatsByCourse();
        Double averageProgress = enrollmentStats.stream()
                .filter(row -> row[0].equals(courseId))
                .map(row -> (Double) row[3])
                .findFirst()
                .orElse(0.0);

        // Compter les étudiants qui ont complété
        long completedStudents = enrollmentRepository.findByCourse_Id(courseId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getCompleted()))
                .count();

        // Récupérer les statistiques des exercices
        List<Exercise> exercises = exerciseRepository.findByCourse_Id(courseId);
        long totalExercises = exercises.size();
        
        List<ExerciseStatsDTO> exerciseStats = exercises.stream()
                .map(this::calculateExerciseStats)
                .collect(Collectors.toList());

        // Calculer la distribution des performances
        PerformanceDistributionDTO performanceDistribution = calculatePerformanceDistribution(courseId);

        TeacherCourseStatsResponse stats = TeacherCourseStatsResponse.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .courseCategory(course.getCategory())
                .totalEnrolled(totalEnrolled)
                .pendingEnrollments(pendingEnrolled)
                .acceptedEnrollments(approvedEnrolled)
                .rejectedEnrollments(rejectedEnrolled)
                .activeStudents(activeStudents)
                .participationRate(participationRate)
                .averageProgress(averageProgress)
                .completedStudents(completedStudents)
                .totalExercises(totalExercises)
                .exerciseStats(exerciseStats)
                .performanceDistribution(performanceDistribution)
                .build();

        return ApiResponse.success("Statistiques du cours récupérées avec succès", stats);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<TeacherCourseStatsResponse>> getAllCoursesStatistics(UUID teacherId) {
        log.info("Récupération des statistiques de tous les cours pour l'enseignant {}", teacherId);

        List<Course> teacherCourses = courseRepository.findByAuthor_Id(teacherId);
        
        List<TeacherCourseStatsResponse> allStats = teacherCourses.stream()
                .map(course -> getCourseStatistics(teacherId, course.getId()).getData())
                .collect(Collectors.toList());

        return ApiResponse.success("Statistiques de tous les cours récupérées avec succès", allStats);
    }

    private ExerciseStatsDTO calculateExerciseStats(Exercise exercise) {
        Integer exerciseId = exercise.getId();
        long submissionCount = studentExerciseRepository.countByExercise_Id(exerciseId);
        Double averageScore = studentExerciseRepository.calculateAverageScore(exerciseId);
        Double minScore = studentExerciseRepository.findMinScore(exerciseId);
        Double maxScore = studentExerciseRepository.findMaxScore(exerciseId);

        return ExerciseStatsDTO.builder()
                .exerciseId(exerciseId)
                .title(exercise.getTitle())
                .submissionCount(submissionCount)
                .averageScore(averageScore != null ? averageScore : 0.0)
                .minScore(minScore != null ? minScore : 0.0)
                .maxScore(maxScore != null ? maxScore : 0.0)
                .maxPossibleScore(exercise.getMaxScore())
                .build();
    }

    private PerformanceDistributionDTO calculatePerformanceDistribution(Integer courseId) {
        // Récupérer toutes les soumissions pour ce cours
        List<StudentExercise> submissions = studentExerciseRepository.findByExercise_Course_Id(courseId);
        
        long excellent = submissions.stream()
                .filter(se -> se.getScore() != null && se.getScore() >= 90)
                .count();
        
        long good = submissions.stream()
                .filter(se -> se.getScore() != null && se.getScore() >= 70 && se.getScore() < 90)
                .count();
        
        long average = submissions.stream()
                .filter(se -> se.getScore() != null && se.getScore() >= 50 && se.getScore() < 70)
                .count();
        
        long poor = submissions.stream()
                .filter(se -> se.getScore() != null && se.getScore() < 50)
                .count();

        return PerformanceDistributionDTO.builder()
                .excellent(excellent)
                .good(good)
                .average(average)
                .poor(poor)
                .build();
    }
}
