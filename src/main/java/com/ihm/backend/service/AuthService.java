// AuthService.java
package com.ihm.backend.service;

import com.ihm.backend.dto.request.AuthenticationRequest;
import com.ihm.backend.dto.request.PasswordResetRequest;
import com.ihm.backend.dto.request.PasswordUpdateRequest;
import com.ihm.backend.dto.request.RegisterRequest;
import com.ihm.backend.dto.request.StudentRegisterRequest;
import com.ihm.backend.dto.request.TeacherRegisterRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;

public interface AuthService {

    ApiResponse<AuthenticationResponse> authenticate(AuthenticationRequest request);

    @Deprecated // Garder pour compatibilité mais déprécier
    ApiResponse<AuthenticationResponse> register(RegisterRequest request);
    
    // Nouveaux endpoints séparés
    ApiResponse<AuthenticationResponse> registerStudent(StudentRegisterRequest request);
    
    ApiResponse<AuthenticationResponse> registerTeacher(TeacherRegisterRequest request);

    ApiResponse<String> requestPasswordReset(PasswordResetRequest request);

    ApiResponse<String> resetPassword(PasswordUpdateRequest request);
}