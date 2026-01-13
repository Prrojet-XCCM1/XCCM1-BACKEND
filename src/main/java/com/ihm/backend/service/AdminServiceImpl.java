package com.ihm.backend.service;

import com.ihm.backend.dto.request.CourseUpdateRequest;
import com.ihm.backend.dto.request.EnrollmentUpdateRequest;
import com.ihm.backend.dto.request.RegisterRequest;
import com.ihm.backend.dto.request.StudentRegisterRequest;
import com.ihm.backend.dto.request.TeacherRegisterRequest;
import com.ihm.backend.dto.request.UserUpdateRequest;
import com.ihm.backend.dto.response.*;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.Enrollment;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.mappers.CourseMapper;
import com.ihm.backend.mappers.EnrollmentMapper;
import com.ihm.backend.mappers.UserMapper;
import com.ihm.backend.repository.CourseRepository;
import com.ihm.backend.repository.EnrollmentRepository;
import com.ihm.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuthService authService;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final EnrollmentMapper enrollmentMapper;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<AdminStatisticsResponse> getStatistics() {
        AdminStatisticsResponse stats = AdminStatisticsResponse.builder()
                .totalUsers(userRepository.count())
                .studentCount(userRepository.countByRole(UserRole.STUDENT))
                .teacherCount(userRepository.countByRole(UserRole.TEACHER))
                .totalCourses(courseRepository.count())
                .publishedCourses(courseRepository.countByStatus(CourseStatus.PUBLISHED))
                .draftCourses(courseRepository.countByStatus(CourseStatus.DRAFT))
                .totalEnrollments(enrollmentRepository.count())
                .pendingEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.PENDING))
                .approvedEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.APPROVED))
                .rejectedEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.REJECTED))
                .build();

        return ApiResponse.success("Statistiques récupérées avec succès", stats);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<EnrollmentStatsResponse> getEnrollmentStatistics() {
        log.info("Récupération des statistiques d'enrollment");

        // Statistiques par statut
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("PENDING", enrollmentRepository.countByStatus(EnrollmentStatus.PENDING));
        byStatus.put("APPROVED", enrollmentRepository.countByStatus(EnrollmentStatus.APPROVED));
        byStatus.put("REJECTED", enrollmentRepository.countByStatus(EnrollmentStatus.REJECTED));

        // Statistiques par cours
        List<Object[]> courseStats = enrollmentRepository.findEnrollmentStatsByCourse();
        List<EnrollmentStatsResponse.CourseEnrollmentStat> byCourse = courseStats.stream()
                .map(row -> EnrollmentStatsResponse.CourseEnrollmentStat.builder()
                        .courseId((Integer) row[0])
                        .courseTitle((String) row[1])
                        .enrollmentCount((Long) row[2])
                        .averageProgress((Double) row[3])
                        .build())
                .collect(Collectors.toList());

        // Progression moyenne et taux de complétion
        Double avgProgress = enrollmentRepository.calculateAverageProgress();
        Double completionRate = enrollmentRepository.calculateCompletionRate();

        // Enrollments récents (7 derniers jours)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recentEnrollments = enrollmentRepository.countByEnrolledAtAfter(sevenDaysAgo);

        EnrollmentStatsResponse stats = EnrollmentStatsResponse.builder()
                .totalEnrollments(enrollmentRepository.count())
                .byStatus(byStatus)
                .byCourse(byCourse)
                .averageProgress(avgProgress != null ? avgProgress : 0.0)
                .completionRate(completionRate != null ? completionRate : 0.0)
                .recentEnrollments(recentEnrollments)
                .build();

        return ApiResponse.success("Statistiques d'enrollment récupérées avec succès", stats);
    }

    @Override
    public ApiResponse<AuthenticationResponse> createStudent(StudentRegisterRequest request) {
        log.info("Admin créant un étudiant: {}", request.getEmail());
        return authService.registerStudent(request);
    }

    @Override
    public ApiResponse<AuthenticationResponse> createTeacher(TeacherRegisterRequest request) {
        log.info("Admin créant un enseignant: {}", request.getEmail());
        return authService.registerTeacher(request);
    }

    @Override
    public ApiResponse<AuthenticationResponse> createAdmin(RegisterRequest request) {
        log.info("Admin créant un autre administrateur: {}", request.getEmail());
        request.setRole(UserRole.ADMIN);
        return authService.register(request);
    }

    // ==================== CRUD Users ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<UserDetailResponse>> getAllUsers() {
        log.info("Récupération de tous les utilisateurs");
        List<UserDetailResponse> users = userRepository.findAll().stream()
                .map(userMapper::toDetailResponse)
                .collect(Collectors.toList());
        return ApiResponse.success("Utilisateurs récupérés avec succès", users);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserDetailResponse> getUserById(UUID id) {
        log.info("Récupération de l'utilisateur avec ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));
        return ApiResponse.success("Utilisateur récupéré avec succès", userMapper.toDetailResponse(user));
    }

    @Override
    @Transactional
    public ApiResponse<UserDetailResponse> updateUser(UUID id, UserUpdateRequest request) {
        log.info("Mise à jour de l'utilisateur avec ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

        updateUserFields(user, request);
        User updatedUser = userRepository.save(user);

        return ApiResponse.success("Utilisateur mis à jour avec succès", userMapper.toDetailResponse(updatedUser));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteUser(UUID id) {
        log.info("Suppression de l'utilisateur avec ID: {}", id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id);
        }
        userRepository.deleteById(id);
        return ApiResponse.success("Utilisateur supprimé avec succès", null);
    }

    // ==================== CRUD Students ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<UserDetailResponse>> getAllStudents() {
        log.info("Récupération de tous les étudiants");
        List<UserDetailResponse> students = userRepository.findByRole(UserRole.STUDENT).stream()
                .map(userMapper::toDetailResponse)
                .collect(Collectors.toList());
        return ApiResponse.success("Étudiants récupérés avec succès", students);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserDetailResponse> getStudentById(UUID id) {
        log.info("Récupération de l'étudiant avec ID: {}", id);
        User student = userRepository.findByIdAndRole(id, UserRole.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant non trouvé avec l'ID: " + id));
        return ApiResponse.success("Étudiant récupéré avec succès", userMapper.toDetailResponse(student));
    }

    @Override
    @Transactional
    public ApiResponse<UserDetailResponse> updateStudent(UUID id, UserUpdateRequest request) {
        log.info("Mise à jour de l'étudiant avec ID: {}", id);
        User student = userRepository.findByIdAndRole(id, UserRole.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant non trouvé avec l'ID: " + id));

        updateUserFields(student, request);
        User updatedStudent = userRepository.save(student);

        return ApiResponse.success("Étudiant mis à jour avec succès", userMapper.toDetailResponse(updatedStudent));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteStudent(UUID id) {
        log.info("Suppression de l'étudiant avec ID: {}", id);
        User student = userRepository.findByIdAndRole(id, UserRole.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant non trouvé avec l'ID: " + id));
        userRepository.delete(student);
        return ApiResponse.success("Étudiant supprimé avec succès", null);
    }

    // ==================== CRUD Teachers ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<UserDetailResponse>> getAllTeachers() {
        log.info("Récupération de tous les enseignants");
        List<UserDetailResponse> teachers = userRepository.findByRole(UserRole.TEACHER).stream()
                .map(userMapper::toDetailResponse)
                .collect(Collectors.toList());
        return ApiResponse.success("Enseignants récupérés avec succès", teachers);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserDetailResponse> getTeacherById(UUID id) {
        log.info("Récupération de l'enseignant avec ID: {}", id);
        User teacher = userRepository.findByIdAndRole(id, UserRole.TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant non trouvé avec l'ID: " + id));
        return ApiResponse.success("Enseignant récupéré avec succès", userMapper.toDetailResponse(teacher));
    }

    @Override
    @Transactional
    public ApiResponse<UserDetailResponse> updateTeacher(UUID id, UserUpdateRequest request) {
        log.info("Mise à jour de l'enseignant avec ID: {}", id);
        User teacher = userRepository.findByIdAndRole(id, UserRole.TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant non trouvé avec l'ID: " + id));

        updateUserFields(teacher, request);
        User updatedTeacher = userRepository.save(teacher);

        return ApiResponse.success("Enseignant mis à jour avec succès", userMapper.toDetailResponse(updatedTeacher));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteTeacher(UUID id) {
        log.info("Suppression de l'enseignant avec ID: {}", id);
        User teacher = userRepository.findByIdAndRole(id, UserRole.TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant non trouvé avec l'ID: " + id));
        userRepository.delete(teacher);
        return ApiResponse.success("Enseignant supprimé avec succès", null);
    }

    // ==================== CRUD Courses ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CourseResponse>> getAllCourses() {
        log.info("Récupération de tous les cours");
        List<CourseResponse> courses = courseRepository.findAll().stream()
                .map(courseMapper::toResponse)
                .collect(Collectors.toList());
        return ApiResponse.success("Cours récupérés avec succès", courses);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CourseResponse> getCourseById(Integer id) {
        log.info("Récupération du cours avec ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé avec l'ID: " + id));
        return ApiResponse.success("Cours récupéré avec succès", courseMapper.toResponse(course));
    }

    @Override
    @Transactional
    public ApiResponse<CourseResponse> updateCourse(Integer id, CourseUpdateRequest request) {
        log.info("Mise à jour du cours avec ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé avec l'ID: " + id));

        if (request.getTitle() != null)
            course.setTitle(request.getTitle());
        if (request.getCategory() != null)
            course.setCategory(request.getCategory());
        if (request.getDescription() != null)
            course.setDescription(request.getDescription());
        if (request.getStatus() != null)
            course.setStatus(request.getStatus());
        if (request.getContent() != null)
            course.setContent(request.getContent());
        if (request.getCoverImage() != null)
            course.setCoverImage(request.getCoverImage());

        Course updatedCourse = courseRepository.save(course);
        return ApiResponse.success("Cours mis à jour avec succès", courseMapper.toResponse(updatedCourse));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteCourse(Integer id) {
        log.info("Suppression du cours avec ID: {}", id);
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cours non trouvé avec l'ID: " + id);
        }
        courseRepository.deleteById(id);
        return ApiResponse.success("Cours supprimé avec succès", null);
    }

    // ==================== CRUD Enrollments ====================

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<EnrollmentDetailResponse>> getAllEnrollments() {
        log.info("Récupération de tous les enrollments");
        List<EnrollmentDetailResponse> enrollments = enrollmentRepository.findAll().stream()
                .map(enrollmentMapper::toDetailResponse)
                .collect(Collectors.toList());
        return ApiResponse.success("Enrollments récupérés avec succès", enrollments);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<EnrollmentDetailResponse> getEnrollmentById(Long id) {
        log.info("Récupération de l'enrollment avec ID: {}", id);
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment non trouvé avec l'ID: " + id));
        return ApiResponse.success("Enrollment récupéré avec succès", enrollmentMapper.toDetailResponse(enrollment));
    }

    @Override
    @Transactional
    public ApiResponse<EnrollmentDetailResponse> updateEnrollment(Long id, EnrollmentUpdateRequest request) {
        log.info("Mise à jour de l'enrollment avec ID: {}", id);
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment non trouvé avec l'ID: " + id));

        if (request.getStatus() != null)
            enrollment.setStatus(request.getStatus());
        if (request.getProgress() != null)
            enrollment.setProgress(request.getProgress());
        if (request.getCompleted() != null)
            enrollment.setCompleted(request.getCompleted());

        Enrollment updatedEnrollment = enrollmentRepository.save(enrollment);
        return ApiResponse.success("Enrollment mis à jour avec succès",
                enrollmentMapper.toDetailResponse(updatedEnrollment));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteEnrollment(Long id) {
        log.info("Suppression de l'enrollment avec ID: {}", id);
        if (!enrollmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Enrollment non trouvé avec l'ID: " + id);
        }
        enrollmentRepository.deleteById(id);
        return ApiResponse.success("Enrollment supprimé avec succès", null);
    }

    // ==================== Helper Methods ====================

    private void updateUserFields(User user, UserUpdateRequest request) {
        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getEmail() != null)
            user.setEmail(request.getEmail());
        if (request.getCity() != null)
            user.setCity(request.getCity());
        if (request.getUniversity() != null)
            user.setUniversity(request.getUniversity());
        if (request.getPhotoUrl() != null)
            user.setPhotoUrl(request.getPhotoUrl());
        if (request.getActive() != null)
            user.setActive(request.getActive());

        // Champs spécifiques aux étudiants
        if (request.getSpecialization() != null)
            user.setSpecialization(request.getSpecialization());

        // Champs spécifiques aux enseignants
        if (request.getGrade() != null)
            user.setGrade(request.getGrade());
        if (request.getSubjects() != null) {
            user.setSubjects(String.join(",", request.getSubjects()));
        }
        if (request.getCertification() != null)
            user.setCertification(request.getCertification());
    }
}
