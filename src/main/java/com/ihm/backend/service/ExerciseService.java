package com.ihm.backend.service;

import com.ihm.backend.dto.request.*;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.ExerciseResponse;
import com.ihm.backend.dto.response.StudentExerciseResponse;
import com.ihm.backend.entity.*;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.mappers.ExerciseMapper;
import com.ihm.backend.mappers.StudentExerciseMapper;
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
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final StudentExerciseRepository studentExerciseRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExerciseMapper exerciseMapper;
    private final StudentExerciseMapper studentExerciseMapper;

    // --- Teacher Operations ---

    @Transactional
    public ApiResponse<ExerciseResponse> createExercise(Integer courseId, UUID teacherId,
            ExerciseCreateRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        if (!course.getAuthor().getId().equals(teacherId)) {
            throw new AccessDeniedException("Vous ne pouvez ajouter des exercices qu'à vos propres cours");
        }

        Exercise exercise = exerciseMapper.toEntity(request);
        exercise.setCourse(course);

        Exercise savedExercise = exerciseRepository.save(exercise);
        return ApiResponse.created("Exercice créé avec succès", exerciseMapper.toResponse(savedExercise));
    }

    @Transactional
    public ApiResponse<ExerciseResponse> updateExercise(Integer exerciseId, UUID teacherId,
            ExerciseUpdateRequest request) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercice non trouvé"));

        if (!exercise.getCourse().getAuthor().getId().equals(teacherId)) {
            throw new AccessDeniedException("Accès refusé");
        }

        exerciseMapper.updateEntityFromRequest(request, exercise);
        Exercise updatedExercise = exerciseRepository.save(exercise);
        return ApiResponse.success("Exercice mis à jour", exerciseMapper.toResponse(updatedExercise));
    }

    @Transactional
    public ApiResponse<Void> deleteExercise(Integer exerciseId, UUID teacherId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercice non trouvé"));

        if (!exercise.getCourse().getAuthor().getId().equals(teacherId)) {
            throw new AccessDeniedException("Accès refusé");
        }

        exerciseRepository.delete(exercise);
        return ApiResponse.success("Exercice supprimé");
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<StudentExerciseResponse>> getSubmissionsForExercise(Integer exerciseId, UUID teacherId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercice non trouvé"));

        if (!exercise.getCourse().getAuthor().getId().equals(teacherId)) {
            throw new AccessDeniedException("Accès refusé");
        }

        List<StudentExerciseResponse> submissions = studentExerciseRepository.findByExercise_Id(exerciseId).stream()
                .map(studentExerciseMapper::toResponse)
                .collect(Collectors.toList());

        return ApiResponse.success("Soumissions récupérées", submissions);
    }

    @Transactional
    public ApiResponse<StudentExerciseResponse> gradeSubmission(Long submissionId, UUID teacherId,
            GradeSubmissionRequest request) {
        StudentExercise submission = studentExerciseRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Soumission non trouvée"));

        if (!submission.getExercise().getCourse().getAuthor().getId().equals(teacherId)) {
            throw new AccessDeniedException("Accès refusé");
        }

        if (request.getScore() > submission.getExercise().getMaxScore()) {
            throw new IllegalArgumentException("La note ne peut pas dépasser la note maximale de l'exercice");
        }

        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback());

        StudentExercise gradedSubmission = studentExerciseRepository.save(submission);
        return ApiResponse.success("Soumission notée", studentExerciseMapper.toResponse(gradedSubmission));
    }

    // --- Student Operations ---

    @Transactional(readOnly = true)
    public ApiResponse<List<ExerciseResponse>> getExercisesForCourse(Integer courseId, UUID studentId) {
        // Verify enrollment
        boolean isEnrolled = enrollmentRepository.existsByCourse_IdAndUser_Id(courseId, studentId);
        if (!isEnrolled) {
            // Check if user is the author (teacher viewing their own course as student
            // view?) or admin
            // Ideally strictly only enrolled students.
            // Allow author to see exercises too (already covered in teacher stats but good
            // for preview)
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));
            if (!course.getAuthor().getId().equals(studentId)) {
                throw new AccessDeniedException("Vous devez être inscrit au cours pour voir les exercices");
            }
        }

        List<ExerciseResponse> exercises = exerciseRepository.findByCourse_Id(courseId).stream()
                .map(exerciseMapper::toResponse)
                .collect(Collectors.toList());

        return ApiResponse.success("Exercices récupérés", exercises);
    }

    @Transactional(readOnly = true)
    public ApiResponse<ExerciseResponse> getExerciseDetails(Integer exerciseId, UUID userId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercice non trouvé"));

        // Check access (enrolled or author)
        boolean isEnrolled = enrollmentRepository.existsByCourse_IdAndUser_Id(exercise.getCourse().getId(), userId);
        boolean isAuthor = exercise.getCourse().getAuthor().getId().equals(userId);

        if (!isEnrolled && !isAuthor) {
            throw new AccessDeniedException("Accès refusé");
        }

        return ApiResponse.success("Détails de l'exercice", exerciseMapper.toResponse(exercise));
    }

    @Transactional
    public ApiResponse<StudentExerciseResponse> submitExercise(Integer exerciseId, UUID studentId,
            StudentExerciseSubmissionRequest request) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercice non trouvé"));

        boolean isEnrolled = enrollmentRepository.existsByCourse_IdAndUser_Id(exercise.getCourse().getId(), studentId);
        if (!isEnrolled) {
            throw new AccessDeniedException("Vous devez être inscrit pour soumettre un exercice");
        }

        // Check if already submitted? - Allow resubmission or new submission.
        // For simplicity let's assume we create a new one or update existing if we want
        // one per student/exercise.
        // Let's implement creating a new submission record (history) or updating
        // latest.
        // Usually simpler to have one submission per student per exercise for now.

        // Check if submission exists
        StudentExercise submission = studentExerciseRepository.findByExercise_Id(exerciseId).stream()
                .filter(s -> s.getStudent().getId().equals(studentId))
                .findFirst()
                .orElse(new StudentExercise());

        if (submission.getId() == null) {
            submission.setExercise(exercise);
            submission.setStudent(userRepository.findById(studentId).orElseThrow());
        }

        submission.setSubmissionUrl(request.getSubmissionUrl());
        submission.setSubmittedAt(LocalDateTime.now());
        // Reset score if re-submitted? Maybe not.

        StudentExercise savedSubmission = studentExerciseRepository.save(submission);
        return ApiResponse.success("Exercice soumis avec succès", studentExerciseMapper.toResponse(savedSubmission));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<StudentExerciseResponse>> getMySubmissions(UUID studentId) {
        List<StudentExerciseResponse> submissions = studentExerciseRepository.findByStudent_Id(studentId).stream()
                .map(studentExerciseMapper::toResponse)
                .collect(Collectors.toList());
        return ApiResponse.success("Mes soumissions", submissions);
    }
}
