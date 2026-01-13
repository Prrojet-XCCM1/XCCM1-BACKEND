package com.ihm.backend.service;

import com.ihm.backend.dto.request.CourseUpdateRequest;
import com.ihm.backend.dto.request.EnrollmentUpdateRequest;
import com.ihm.backend.dto.request.RegisterRequest;
import com.ihm.backend.dto.request.StudentRegisterRequest;
import com.ihm.backend.dto.request.TeacherRegisterRequest;
import com.ihm.backend.dto.request.UserUpdateRequest;
import com.ihm.backend.dto.response.AdminStatisticsResponse;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;
import com.ihm.backend.dto.response.CourseResponse;
import com.ihm.backend.dto.response.EnrollmentDetailResponse;
import com.ihm.backend.dto.response.EnrollmentStatsResponse;
import com.ihm.backend.dto.response.UserDetailResponse;

import java.util.List;
import java.util.UUID;

public interface AdminService {

    // Statistiques générales
    ApiResponse<AdminStatisticsResponse> getStatistics();

    // Statistiques d'enrollment détaillées
    ApiResponse<EnrollmentStatsResponse> getEnrollmentStatistics();

    // Création d'utilisateurs
    ApiResponse<AuthenticationResponse> createStudent(StudentRegisterRequest request);

    ApiResponse<AuthenticationResponse> createTeacher(TeacherRegisterRequest request);

    ApiResponse<AuthenticationResponse> createAdmin(RegisterRequest request);

    // CRUD Users (tous types)
    ApiResponse<List<UserDetailResponse>> getAllUsers();

    ApiResponse<UserDetailResponse> getUserById(UUID id);

    ApiResponse<UserDetailResponse> updateUser(UUID id, UserUpdateRequest request);

    ApiResponse<Void> deleteUser(UUID id);

    // CRUD Students
    ApiResponse<List<UserDetailResponse>> getAllStudents();

    ApiResponse<UserDetailResponse> getStudentById(UUID id);

    ApiResponse<UserDetailResponse> updateStudent(UUID id, UserUpdateRequest request);

    ApiResponse<Void> deleteStudent(UUID id);

    // CRUD Teachers
    ApiResponse<List<UserDetailResponse>> getAllTeachers();

    ApiResponse<UserDetailResponse> getTeacherById(UUID id);

    ApiResponse<UserDetailResponse> updateTeacher(UUID id, UserUpdateRequest request);

    ApiResponse<Void> deleteTeacher(UUID id);

    // CRUD Courses
    ApiResponse<List<CourseResponse>> getAllCourses();

    ApiResponse<CourseResponse> getCourseById(Integer id);

    ApiResponse<CourseResponse> updateCourse(Integer id, CourseUpdateRequest request);

    ApiResponse<Void> deleteCourse(Integer id);

    // CRUD Enrollments
    ApiResponse<List<EnrollmentDetailResponse>> getAllEnrollments();

    ApiResponse<EnrollmentDetailResponse> getEnrollmentById(Long id);

    ApiResponse<EnrollmentDetailResponse> updateEnrollment(Long id, EnrollmentUpdateRequest request);

    ApiResponse<Void> deleteEnrollment(Long id);
}
