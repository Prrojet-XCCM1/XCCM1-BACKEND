package com.ihm.backend.service;

import com.ihm.backend.dto.request.*;
import com.ihm.backend.dto.response.*;
import com.ihm.backend.entity.*;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.*;
import com.ihm.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Override
    public ApiResponse<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

            // Vérifier le mot de passe
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ApiResponse.unauthorized("Email ou mot de passe incorrect", null);
            }

            if (!user.isEnabled()) {
                return ApiResponse.unauthorized("Compte désactivé ou non vérifié", null);
            }

            // Mettre à jour lastLogin
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            String jwt = jwtService.generateToken(user);
            AuthenticationResponse authResponse = AuthenticationResponse.fromUser(user, jwt);

            return ApiResponse.success("Connexion réussie", authResponse);

        } catch (UsernameNotFoundException e) {
            return ApiResponse.unauthorized("Email ou mot de passe incorrect", null);
        }
    }

    @Override
    @Transactional
    public ApiResponse<AuthenticationResponse> register(RegisterRequest request) {
        log.info("Tentative d'inscription pour: {}", request.getEmail());
        
        // Validation des mots de passe
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.badRequest("Les mots de passe ne correspondent pas", null);
        }

        // Vérification email unique
        if (userRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.conflict("Cet email est déjà utilisé", null);
        }

        // Construction de l'utilisateur selon le rôle
        User user = buildUser(request);

        // Hash du mot de passe
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRegistrationDate(LocalDateTime.now());
        user.setActive(true);
        user.setVerified(true);

        // Sauvegarde
        User saved = userRepository.save(user);
        log.info("Utilisateur créé: {} avec le rôle {}", saved.getEmail(), saved.getRole());

        // Génération token
        String jwt = jwtService.generateToken(saved);
        AuthenticationResponse response = AuthenticationResponse.fromUser(saved, jwt);

        return ApiResponse.created("Inscription réussie", response);
    }

    /**
     * Construit un utilisateur à partir de la requête d'inscription
     * Gère les champs spécifiques selon le rôle
     */
    private User buildUser(RegisterRequest r) {
        User user = User.builder()
                .email(r.getEmail())
                .firstName(r.getFirstName())
                .lastName(r.getLastName())
                .role(r.getRole())
                .photoUrl(r.getPhotoUrl())
                .city(r.getCity())
                .university(r.getUniversity())
                .build();

        // Champs spécifiques selon le rôle
        if (r.getRole() == UserRole.STUDENT) {
            user.setSpecialization(r.getSpecialization());
        } else if (r.getRole() == UserRole.TEACHER) {
            user.setGrade(r.getGrade());
            user.setCertification(r.getCertification());
            
            // Conversion de List<String> vers String (stockage CSV ou JSON)
            if (r.getSubjects() != null && !r.getSubjects().isEmpty()) {
                user.setSubjects(String.join(",", r.getSubjects()));
            }
        }

        return user;
    }

    @Override
    public ApiResponse<String> requestPasswordReset(PasswordResetRequest request) {
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

        emailService.sendPasswordResetEmail(user.getEmail(), token);

        return ApiResponse.success("Email de réinitialisation envoyé");
    }

    @Override
    @Transactional
    public ApiResponse<String> resetPassword(PasswordUpdateRequest request) {
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

    /**
     * Inscription spécifique pour les étudiants
     */
    @Override
    @Transactional
    public ApiResponse<AuthenticationResponse> registerStudent(StudentRegisterRequest request) {
        log.info("Tentative d'inscription étudiant pour: {}", request.getEmail());
        
        // Validation des mots de passe
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.badRequest("Les mots de passe ne correspondent pas", null);
        }

        // Vérification email unique
        if (userRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.conflict("Cet email est déjà utilisé", null);
        }

        // Construction de l'utilisateur étudiant
        User student = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.STUDENT)
                .photoUrl(request.getPhotoUrl())
                .city(request.getCity())
                .university(request.getUniversity())
                .specialization(request.getSpecialization())
                .password(passwordEncoder.encode(request.getPassword()))
                .registrationDate(LocalDateTime.now())
                .active(true)
                .verified(true)
                .build();

        User saved = userRepository.save(student);
        log.info("Étudiant créé: {}", saved.getEmail());

        String jwt = jwtService.generateToken(saved);
        AuthenticationResponse response = AuthenticationResponse.fromUser(saved, jwt);

        return ApiResponse.created("Inscription étudiant réussie", response);
    }

    /**
     * Inscription spécifique pour les enseignants
     */
    @Override
    @Transactional
    public ApiResponse<AuthenticationResponse> registerTeacher(TeacherRegisterRequest request) {
        log.info("Tentative d'inscription enseignant pour: {}", request.getEmail());
        
        // Validation des mots de passe
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.badRequest("Les mots de passe ne correspondent pas", null);
        }

        // Vérification email unique
        if (userRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.conflict("Cet email est déjà utilisé", null);
        }

        // Conversion de List<String> vers String pour subjects
        String subjectsStr = null;
        if (request.getSubjects() != null && !request.getSubjects().isEmpty()) {
            subjectsStr = String.join(",", request.getSubjects());
        }

        // Construction de l'utilisateur enseignant
        User teacher = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.TEACHER)
                .photoUrl(request.getPhotoUrl())
                .city(request.getCity())
                .university(request.getUniversity())
                .grade(request.getGrade())
                .subjects(subjectsStr)
                .certification(request.getCertification())
                .password(passwordEncoder.encode(request.getPassword()))
                .registrationDate(LocalDateTime.now())
                .active(true)
                .verified(true)
                .build();

        User saved = userRepository.save(teacher);
        log.info("Enseignant créé: {}", saved.getEmail());

        String jwt = jwtService.generateToken(saved);
        AuthenticationResponse response = AuthenticationResponse.fromUser(saved, jwt);

        return ApiResponse.created("Inscription enseignant réussie", response);
    }
}
