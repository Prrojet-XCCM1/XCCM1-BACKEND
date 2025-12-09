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

    ApiResponse<AuthenticationResponse> register(RegisterRequest request);  // ← plus ?

    ApiResponse<String> requestPasswordReset(PasswordResetRequest request); // ← String ou Void

    ApiResponse<String> resetPassword(PasswordUpdateRequest request);       // ← String
}