package cm.enspy.xccm.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cm.enspy.xccm.domain.entity.*;
import cm.enspy.xccm.domain.enums.UserRole;
import cm.enspy.xccm.domain.dto.request.*;
import cm.enspy.xccm.domain.dto.response.AuthenticationResponse;
import cm.enspy.xccm.repository.*;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ReactiveAuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    public Mono<AuthenticationResponse> register(RegisterRequest request) {
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Un compte avec cet email existe déjà"));
                    }
                    
                    if (!request.getPassword().equals(request.getConfirmPassword())) {
                        return Mono.error(new RuntimeException("Les mots de passe ne correspondent pas"));
                    }
                    
                    return createUser(request);
                });
    }

    private Mono<AuthenticationResponse> createUser(RegisterRequest request) {
        return Mono.defer(() -> {
            User user = createUserFromRequest(request);
            return userRepository.save(user)
                    .map(savedUser -> {
                        String jwtToken = jwtService.generateToken(savedUser);
                        return AuthenticationResponse.fromUser(savedUser, jwtToken);
                    });
        });
    }

    private User createUserFromRequest(RegisterRequest request) {
        User user;
        
        if (request.getRole() == UserRole.STUDENT) {
            user = Student.builder()
                    .id(UUID.randomUUID())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRole())
                    .photoUrl(request.getPhotoUrl() != null ? request.getPhotoUrl() : "/images/default-profile.avif")
                    .city(request.getCity())
                    .university(request.getUniversity())
                    .registrationDate(LocalDateTime.now())
                    .promotion(request.getPromotion())
                    .specialization(request.getSpecialization())
                    .level(request.getLevel())
                    .averageGrade(request.getAverageGrade() != null ? Double.parseDouble(request.getAverageGrade()) : null)
                    .currentSemester(request.getCurrentSemester() != null ? Integer.parseInt(request.getCurrentSemester()) : null)
                    .major(request.getMajor())
                    .minor(request.getMinor())
                    .interests(convertListToString(request.getInterests()))
                    .activities(convertListToString(request.getActivities()))
                    .studyField(request.getStudyField())
                    .academicYear(request.getAcademicYear())
                    .build();
        } else {
            user = Teacher.builder()
                    .id(UUID.randomUUID())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRole())
                    .photoUrl(request.getPhotoUrl() != null ? request.getPhotoUrl() : "/images/default-profile.avif")
                    .city(request.getCity())
                    .university(request.getUniversity())
                    .registrationDate(LocalDateTime.now())
                    .grade(request.getGrade())
                    .certification(request.getCertification())
                    .subjects(convertListToString(request.getSubjects()))
                    .teachingGrades(convertListToString(request.getTeachingGrades()))
                    .teachingGoal(request.getTeachingGoal())
                    .department(request.getDepartment())
                    .yearsOfExperience(request.getYearsOfExperience())
                    .officeLocation(request.getOfficeLocation())
                    .officeHours(request.getOfficeHours())
                    .build();
        }
        
        return user;
    }

    private String convertListToString(java.util.List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
    }

    public Mono<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        )
        .then(findUserByEmailAndRole(request.getEmail(), request.getRole()))
        .flatMap(user -> 
            userRepository.updateLastLogin(user.getId())
                .thenReturn(user)
        )
        .map(user -> {
            String jwtToken = jwtService.generateToken(user);
            return AuthenticationResponse.fromUser(user, jwtToken);
        });
    }

    private Mono<User> findUserByEmailAndRole(String email, UserRole role) {
        if (role == UserRole.STUDENT) {
            return studentRepository.findByEmail(email)
                    .cast(User.class);
        } else {
            return teacherRepository.findByEmail(email)
                    .cast(User.class);
        }
    }

    public Mono<String> requestPasswordReset(PasswordResetRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new RuntimeException("Aucun compte associé à cet email")))
                .flatMap(user -> 
                    passwordResetTokenRepository.markAllTokensAsUsed(user.getId())
                        .then(generateResetToken(user))
                )
                .flatMap(token -> 
                    emailService.sendPasswordResetEmail(request.getEmail(), token)
                        .thenReturn("Un email de réinitialisation a été envoyé")
                )
                .onErrorResume(e -> Mono.error(new RuntimeException("Erreur lors de la demande de réinitialisation")));
    }

    private Mono<String> generateResetToken(User user) {
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .token(token)
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        return passwordResetTokenRepository.save(resetToken)
                .thenReturn(token);
    }

    public Mono<String> resetPassword(PasswordUpdateRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Mono.error(new RuntimeException("Les mots de passe ne correspondent pas"));
        }

        return passwordResetTokenRepository.findByToken(request.getToken())
                .switchIfEmpty(Mono.error(new RuntimeException("Token invalide")))
                .flatMap(resetToken -> {
                    if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                        return Mono.error(new RuntimeException("Le token a expiré"));
                    }
                    if (Boolean.TRUE.equals(resetToken.getUsed())) {
                        return Mono.error(new RuntimeException("Le token a déjà été utilisé"));
                    }
                    
                    return userRepository.findById(resetToken.getUserId())
                            .flatMap(user -> {
                                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                                return userRepository.save(user)
                                        .then(passwordResetTokenRepository.markAllTokensAsUsed(user.getId()))
                                        .then(passwordResetTokenRepository.deleteAllExpiredSince(LocalDateTime.now()))
                                        .thenReturn("Mot de passe réinitialisé avec succès");
                            });
                });
    }
}