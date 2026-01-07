package com.ihm.backend.service;

import com.ihm.backend.dto.request.StudentRegisterRequest;
import com.ihm.backend.dto.request.TeacherRegisterRequest;
import com.ihm.backend.dto.response.AdminStatisticsResponse;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;

public interface AdminService {

    ApiResponse<AdminStatisticsResponse> getStatistics();

    ApiResponse<AuthenticationResponse> createStudent(StudentRegisterRequest request);

    ApiResponse<AuthenticationResponse> createTeacher(TeacherRegisterRequest request);
}
