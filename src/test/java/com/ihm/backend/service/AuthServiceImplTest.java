package com.ihm.backend.service;

import com.ihm.backend.dto.request.AuthenticationRequest;
import com.ihm.backend.dto.request.PasswordResetRequest;
import com.ihm.backend.dto.request.PasswordUpdateRequest;
import com.ihm.backend.dto.request.StudentRegisterRequest;
import com.ihm.backend.dto.request.TeacherRegisterRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;
import com.ihm.backend.entity.PasswordResetToken;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.repository.elasticsearch.UserSearchRepository;
import com.ihm.backend.repository.jpa.PasswordResetTokenRepository;
import com.ihm.backend.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl — Tests unitaires")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private NotificationService notificationService;
    @Mock private UserSearchRepository userSearchRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeStudent;
    private User activeTeacher;

    @BeforeEach
    void setUp() {
        activeStudent = User.builder()
                .id(UUID.randomUUID())
                .email("student@test.com")
                .password("$2a$10$hashed")
                .role(UserRole.STUDENT)
                .firstName("Jean")
                .lastName("Dupont")
                .active(true)
                .verified(true)
                .build();

        activeTeacher = User.builder()
                .id(UUID.randomUUID())
                .email("teacher@test.com")
                .password("$2a$10$hashed")
                .role(UserRole.TEACHER)
                .firstName("Marie")
                .lastName("Curie")
                .active(true)
                .verified(true)
                .build();
    }

    // =========================================================================
    //  AUTHENTIFICATION
    // =========================================================================
    @Nested
    @DisplayName("authenticate — Connexion")
    class Authenticate {

        @Test
        @DisplayName("✅ Identifiants valides → token JWT retourné")
        void validCredentials_returnsToken() {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(activeStudent));
            when(passwordEncoder.matches("password123", activeStudent.getPassword())).thenReturn(true);
            when(jwtService.generateToken(activeStudent)).thenReturn("jwt-token");
            when(userRepository.save(any())).thenReturn(activeStudent);

            ApiResponse<AuthenticationResponse> response =
                    authService.authenticate(new AuthenticationRequest("student@test.com", "password123"));

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getToken()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("✅ Mot de passe incorrect → 401")
        void wrongPassword_returns401() {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(activeStudent));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            ApiResponse<AuthenticationResponse> response =
                    authService.authenticate(new AuthenticationRequest("student@test.com", "wrong"));

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("✅ Email inconnu → 401 (pas de fuite d'information)")
        void unknownEmail_returns401() {
            when(userRepository.findByEmail(anyString()))
                    .thenThrow(new UsernameNotFoundException("not found"));

            ApiResponse<AuthenticationResponse> response =
                    authService.authenticate(new AuthenticationRequest("nobody@test.com", "pwd"));

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("✅ Compte désactivé → 401")
        void disabledAccount_returns401() {
            activeStudent.setActive(false);
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(activeStudent));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            ApiResponse<AuthenticationResponse> response =
                    authService.authenticate(new AuthenticationRequest("student@test.com", "password123"));

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("✅ Connexion réussie → lastLogin mis à jour")
        void login_updatesLastLogin() {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(activeStudent));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateToken(any())).thenReturn("token");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenReturn(activeStudent);

            authService.authenticate(new AuthenticationRequest("student@test.com", "password123"));

            assertThat(captor.getValue().getLastLogin()).isNotNull();
        }
    }

    // =========================================================================
    //  INSCRIPTION ÉTUDIANT
    // =========================================================================
    @Nested
    @DisplayName("registerStudent — Inscription étudiant")
    class RegisterStudent {

        private StudentRegisterRequest validRequest() {
            StudentRegisterRequest req = new StudentRegisterRequest();
            req.setEmail("new@student.com");
            req.setPassword("password123");
            req.setConfirmPassword("password123");
            req.setFirstName("Alice");
            req.setLastName("Martin");
            return req;
        }

        @Test
        @DisplayName("✅ Inscription valide → 201 + token JWT")
        void validRegistration_returns201() {
            when(userRepository.existsByEmail("new@student.com")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            when(userRepository.save(any())).thenReturn(activeStudent);
            when(userSearchRepository.save(any())).thenReturn(activeStudent);
            when(jwtService.generateToken(any())).thenReturn("jwt-token");

            ApiResponse<AuthenticationResponse> response = authService.registerStudent(validRequest());

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getCode()).isEqualTo(201);
            verify(notificationService).sendWelcomeEmail(any());
        }

        @Test
        @DisplayName("✅ Email déjà utilisé → 409")
        void duplicateEmail_returns409() {
            when(userRepository.existsByEmail("new@student.com")).thenReturn(true);

            StudentRegisterRequest req = validRequest();
            ApiResponse<AuthenticationResponse> response = authService.registerStudent(req);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(409);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Mots de passe non concordants → 400")
        void passwordMismatch_returns400() {
            StudentRegisterRequest req = validRequest();
            req.setConfirmPassword("different");

            ApiResponse<AuthenticationResponse> response = authService.registerStudent(req);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(400);
        }

        @Test
        @DisplayName("✅ Rôle de l'utilisateur créé est bien STUDENT")
        void createdUser_hasStudentRole() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(jwtService.generateToken(any())).thenReturn("token");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenReturn(activeStudent);
            when(userSearchRepository.save(any())).thenReturn(activeStudent);

            authService.registerStudent(validRequest());

            assertThat(captor.getValue().getRole()).isEqualTo(UserRole.STUDENT);
        }
    }

    // =========================================================================
    //  INSCRIPTION ENSEIGNANT
    // =========================================================================
    @Nested
    @DisplayName("registerTeacher — Inscription enseignant")
    class RegisterTeacher {

        private TeacherRegisterRequest validRequest() {
            TeacherRegisterRequest req = new TeacherRegisterRequest();
            req.setEmail("new@teacher.com");
            req.setPassword("password123");
            req.setConfirmPassword("password123");
            req.setFirstName("Paul");
            req.setLastName("Dupont");
            req.setGrade("Professeur");
            req.setSubjects(java.util.List.of("Maths", "Physique"));
            return req;
        }

        @Test
        @DisplayName("✅ Inscription valide → 201 + rôle TEACHER")
        void validRegistration_returnsTeacherRole() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(jwtService.generateToken(any())).thenReturn("token");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenReturn(activeTeacher);
            when(userSearchRepository.save(any())).thenReturn(activeTeacher);

            ApiResponse<AuthenticationResponse> response = authService.registerTeacher(validRequest());

            assertThat(response.isSuccess()).isTrue();
            assertThat(captor.getValue().getRole()).isEqualTo(UserRole.TEACHER);
            assertThat(captor.getValue().getSubjects()).contains("Maths");
        }

        @Test
        @DisplayName("✅ Subjects null → subjects reste null en DB")
        void nullSubjects_savedAsNull() {
            TeacherRegisterRequest req = validRequest();
            req.setSubjects(null);

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(jwtService.generateToken(any())).thenReturn("token");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenReturn(activeTeacher);
            when(userSearchRepository.save(any())).thenReturn(activeTeacher);

            authService.registerTeacher(req);

            assertThat(captor.getValue().getSubjects()).isNull();
        }
    }

    // =========================================================================
    //  MOT DE PASSE OUBLIÉ
    // =========================================================================
    @Nested
    @DisplayName("requestPasswordReset — Mot de passe oublié")
    class RequestPasswordReset {

        @Test
        @DisplayName("✅ Email valide → token créé + email envoyé")
        void validEmail_createsTokenAndSendsEmail() {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(activeStudent));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse<String> response =
                    authService.requestPasswordReset(new PasswordResetRequest("student@test.com"));

            assertThat(response.isSuccess()).isTrue();

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());
            PasswordResetToken savedToken = captor.getValue();
            assertThat(savedToken.getToken()).isNotBlank();
            assertThat(savedToken.getUsed()).isFalse();
            assertThat(savedToken.getExpiryDate()).isAfter(LocalDateTime.now());

            verify(notificationService).sendPasswordResetEmail(eq(activeStudent), anyString());
        }

        @Test
        @DisplayName("✅ Email inconnu → ResourceNotFoundException")
        void unknownEmail_throwsException() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    authService.requestPasswordReset(new PasswordResetRequest("nobody@test.com")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("✅ Token généré expire dans 1 heure")
        void generatedToken_expiresInOneHour() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(activeStudent));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.requestPasswordReset(new PasswordResetRequest("student@test.com"));

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());

            LocalDateTime expiry = captor.getValue().getExpiryDate();
            assertThat(expiry).isBetween(
                    LocalDateTime.now().plusMinutes(59),
                    LocalDateTime.now().plusMinutes(61));
        }
    }

    // =========================================================================
    //  RÉINITIALISATION DE MOT DE PASSE
    // =========================================================================
    @Nested
    @DisplayName("resetPassword — Réinitialisation de mot de passe")
    class ResetPassword {

        private PasswordResetToken validToken() {
            return PasswordResetToken.builder()
                    .id(UUID.randomUUID())
                    .token("valid-token-123")
                    .userId(activeStudent.getId())
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build();
        }

        @Test
        @DisplayName("✅ Token valide + mots de passe concordants → mot de passe mis à jour")
        void validToken_updatesPassword() {
            PasswordResetToken token = validToken();
            when(tokenRepository.findByToken("valid-token-123")).thenReturn(Optional.of(token));
            when(userRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
            when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$10$newHashed");
            when(userRepository.save(any())).thenReturn(activeStudent);
            when(tokenRepository.save(any())).thenReturn(token);

            PasswordUpdateRequest request = new PasswordUpdateRequest(
                    "valid-token-123", "NewPass123", "NewPass123");
            ApiResponse<String> response = authService.resetPassword(request);

            assertThat(response.isSuccess()).isTrue();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$10$newHashed");

            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getUsed()).isTrue();
        }

        @Test
        @DisplayName("✅ Token inconnu → ResourceNotFoundException")
        void unknownToken_throwsException() {
            when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(
                    new PasswordUpdateRequest("bad-token", "pass", "pass")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("✅ Token expiré → 400")
        void expiredToken_returns400() {
            PasswordResetToken expiredToken = PasswordResetToken.builder()
                    .token("expired-token")
                    .userId(activeStudent.getId())
                    .expiryDate(LocalDateTime.now().minusHours(1))
                    .used(false)
                    .build();
            when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

            ApiResponse<String> response = authService.resetPassword(
                    new PasswordUpdateRequest("expired-token", "NewPass123", "NewPass123"));

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(400);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Token déjà utilisé → 400")
        void alreadyUsedToken_returns400() {
            PasswordResetToken usedToken = PasswordResetToken.builder()
                    .token("used-token")
                    .userId(activeStudent.getId())
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .used(true)
                    .build();
            when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

            ApiResponse<String> response = authService.resetPassword(
                    new PasswordUpdateRequest("used-token", "NewPass123", "NewPass123"));

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(400);
        }

        @Test
        @DisplayName("✅ Mots de passe non concordants → 400")
        void passwordMismatch_returns400() {
            PasswordResetToken token = validToken();
            when(tokenRepository.findByToken("valid-token-123")).thenReturn(Optional.of(token));

            ApiResponse<String> response = authService.resetPassword(
                    new PasswordUpdateRequest("valid-token-123", "Pass1", "Pass2"));

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(400);
            verify(userRepository, never()).save(any());
        }
    }
}
