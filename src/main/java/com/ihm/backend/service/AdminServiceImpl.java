package com.ihm.backend.service;

import com.ihm.backend.dto.request.RegisterRequest;
import com.ihm.backend.dto.request.StudentRegisterRequest;
import com.ihm.backend.dto.request.TeacherRegisterRequest;
import com.ihm.backend.dto.response.AdminStatisticsResponse;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;
import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.repository.CourseRepository;
import com.ihm.backend.repository.EnrollmentRepository;
import com.ihm.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuthService authService;

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
}
