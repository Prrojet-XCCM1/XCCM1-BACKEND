// AuthService.java
package com.ihm.backend.service;

import com.ihm.backend.domain.dto.request.AuthenticationRequest;
import com.ihm.backend.domain.dto.request.PasswordResetRequest;
import com.ihm.backend.domain.dto.request.PasswordUpdateRequest;
import com.ihm.backend.domain.dto.request.RegisterRequest;
import com.ihm.backend.domain.dto.response.ApiResponse;
import com.ihm.backend.domain.dto.response.AuthenticationResponse;

public interface AuthService {

    ApiResponse<AuthenticationResponse> authenticate(AuthenticationRequest request);

    ApiResponse<?> register(RegisterRequest request);

    ApiResponse<?> requestPasswordReset(PasswordResetRequest request);

    ApiResponse<?> resetPassword(PasswordUpdateRequest request);

    ApiResponse<?> verifyEmail(String token); // si tu veux plus tard
}