// AuthServiceImpl.java (implémentation concrète)
package com.ihm.backend.service;

import com.ihm.backend.domain.dto.request.*;
import com.ihm.backend.domain.dto.response.*;
import com.ihm.backend.domain.entity.*;
import com.ihm.backend.domain.entity.User;
import com.ihm.backend.domain.enums.UserRole;
import com.ihm.backend.repository.*;
import com.ihm.backend.service.AuthService;
import com.ihm.backend.exception.*;
import com.ihm.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService; // tu le crées plus tard

    @Override
    public ApiResponse<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

            if (!user.isEnabled()) {
                return ApiResponse.unauthorized("Compte désactivé ou non vérifié", null);
            }

            String jwt = jwtService.generateToken(user);
            AuthenticationResponse authResponse = AuthenticationResponse.fromUser(user, jwt);

            return ApiResponse.success("Connexion réussie", authResponse);

        } catch (BadCredentialsException e) {
            return ApiResponse.unauthorized("Email ou mot de passe incorrect", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.badRequest("Les mots de passe ne correspondent pas", null);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.conflict("Cet email est déjà utilisé", null);
        }

        User user = switch (request.getRole()) {
            case STUDENT -> buildStudent(request);
            case TEACHER -> buildTeacher(request);
            default -> throw new IllegalArgumentException("Rôle non supporté: " + request.getRole());
        };

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRegistrationDate(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        String jwt = jwtService.generateToken(saved);
        AuthenticationResponse response = AuthenticationResponse.fromUser(saved, jwt);

        // TODO : envoyer email de bienvenue/vérification

        return ApiResponse.created("Inscription réussie", response);
    }

    private Student buildStudent(RegisterRequest r) {
        return Student.builder()
            .email(r.getEmail())
            .firstName(r.getFirstName())
            .lastName(r.getLastName())
            .role(UserRole.STUDENT)
            .photoUrl(r.getPhotoUrl())
            .city(r.getCity())
            .university(r.getUniversity())
            .promotion(r.getPromotion())
            .specialization(r.getSpecialization())
            .level(r.getLevel())
            .averageGrade(Double.valueOf(r.getAverageGrade() != null ? r.getAverageGrade() : "0"))
            .currentSemester(r.getCurrentSemester() != null ? Integer.valueOf(r.getCurrentSemester()) : null)
            .major(r.getMajor())
            .minor(r.getMinor())
            .studyField(r.getStudyField())
            .academicYear(r.getAcademicYear())
            .interests(r.getInterests() != null ? String.join(",", r.getInterests()) : null)
            .activities(r.getActivities() != null ? String.join(",", r.getActivities()) : null)
            .build();
    }

    private Teacher buildTeacher(RegisterRequest r) {
        return Teacher.builder()
            .email(r.getEmail())
            .firstName(r.getFirstName())
            .lastName(r.getLastName())
            .role(UserRole.TEACHER)
            .photoUrl(r.getPhotoUrl())
            .city(r.getCity())
            .university(r.getUniversity())
            .grade(r.getGrade())
            .certification(r.getCertification())
            .teachingGoal(r.getTeachingGoal())
            .subjects(r.getSubjects() != null ? String.join(",", r.getSubjects()) : null)
            .teachingGrades(r.getTeachingGrades() != null ? String.join(",", r.getTeachingGrades()) : null)
            .department(r.getDepartment())
            .yearsOfExperience(r.getYearsOfExperience())
            .officeLocation(r.getOfficeLocation())
            .officeHours(r.getOfficeHours())
            .build();
    }

    @Override
    public ApiResponse<?> requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .token(token)
            .userId(user.getId())
            .expiryDate(LocalDateTime.now().plusHours(1))
            .used(false)
            .build();

        tokenRepository.save(resetToken);

        // TODO : envoyer email avec lien : /reset-password?token=xxx
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        return ApiResponse.success("Email de réinitialisation envoyé");
    }

    @Override
    @Transactional
    public ApiResponse<?> resetPassword(PasswordUpdateRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
            .orElseThrow(() -> new ResourceNotFoundException("Token invalide ou expiré"));

        if (token.isExpired() || token.getUsed()) {
            return ApiResponse.badRequest("Token expiré ou déjà utilisé", null);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.badRequest("Les mots de passe ne correspondent pas", null);
        }

        User user = userRepository.findById(token.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        return ApiResponse.success("Mot de passe réinitialisé avec succès");
    }
}