package com.ihm.backend.service;

import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.repository.jpa.UserRepository;
import com.ihm.backend.service.JwtService;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour LtiAccountMappingService.
 * Couvre le JIT Provisioning (Just-In-Time) lors du lancement LTI 1.3 :
 * - Utilisateur existant par moodle_sub → connexion directe
 * - Utilisateur existant par email → liaison LTI
 * - Nouvel utilisateur → création du compte
 * - Pas d'email dans le JWT → email synthétique @moodle.lti
 */
@ExtendWith(MockitoExtension.class)
class LtiAccountMappingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LtiAccountMappingService service;

    private final String MOODLE_SUB = "moodle-sub-123";
    private final String EMAIL = "teacher@moodle.local";

    // =========================================================================
    // CAS 1 : Utilisateur déjà lié par moodle_sub (connexion rapide)
    // =========================================================================

    @Nested
    @DisplayName("Utilisateur existant via moodle_sub")
    class ExistingUserByMoodleSub {

        @Test
        @DisplayName("resolveAndGenerateToken — Utilisateur trouvé par moodle_sub → retourne token JWT")
        void existingUserByMoodleSub_returnsToken() throws Exception {
            User existingUser = User.builder()
                    .id(UUID.randomUUID())
                    .email(EMAIL)
                    .moodleSub(MOODLE_SUB)
                    .role(UserRole.TEACHER)
                    .active(true)
                    .build();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(MOODLE_SUB)
                    .claim("email", EMAIL)
                    .claim("given_name", "Marie")
                    .claim("family_name", "Dupont")
                    .claim("https://purl.imsglobal.org/spec/lti/claim/roles",
                            List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor"))
                    .build();

            when(userRepository.findByMoodleSub(MOODLE_SUB)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(existingUser);
            when(jwtService.generateToken(existingUser)).thenReturn("xccm1-token-abc");

            String token = service.resolveAndGenerateToken(claims);

            assertEquals("xccm1-token-abc", token);
            verify(userRepository).findByMoodleSub(MOODLE_SUB);
            verify(jwtService).generateToken(existingUser);
            // Pas de création de compte
            verify(userRepository, never()).findByEmail(anyString());
        }
    }

    // =========================================================================
    // CAS 2 : Utilisateur existant par email (liaison LTI)
    // =========================================================================

    @Nested
    @DisplayName("Liaison LTI par email existant")
    class ExistingUserByEmail {

        @Test
        @DisplayName("resolveAndGenerateToken — Email connu, moodle_sub nouveau → liaison du compte existant")
        void existingUserByEmail_linksAccount() throws Exception {
            User existingUser = User.builder()
                    .id(UUID.randomUUID())
                    .email(EMAIL)
                    .moodleSub(null) // Pas encore lié
                    .role(UserRole.TEACHER)
                    .build();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(MOODLE_SUB)
                    .claim("email", EMAIL)
                    .claim("given_name", "Marie")
                    .claim("family_name", "Dupont")
                    .claim("https://purl.imsglobal.org/spec/lti/claim/roles",
                            List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor"))
                    .build();

            when(userRepository.findByMoodleSub(MOODLE_SUB)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenReturn(existingUser);
            when(jwtService.generateToken(existingUser)).thenReturn("xccm1-token-linked");

            String token = service.resolveAndGenerateToken(claims);

            assertEquals("xccm1-token-linked", token);
            // Le moodle_sub doit avoir été lié
            assertEquals(MOODLE_SUB, existingUser.getMoodleSub());
        }
    }

    // =========================================================================
    // CAS 3 : Nouvel utilisateur (JIT Provisioning)
    // =========================================================================

    @Nested
    @DisplayName("JIT Provisioning — Nouvel utilisateur")
    class JitProvisioning {

        @Test
        @DisplayName("resolveAndGenerateToken — Email inconnu → crée un nouveau compte TEACHER")
        void newUserWithEmail_createsTeacherAccount() throws Exception {
            String newEmail = "new.instructor@moodle.local";
            String newSub = "moodle-sub-new-999";

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(newSub)
                    .claim("email", newEmail)
                    .claim("given_name", "Paul")
                    .claim("family_name", "Martin")
                    .claim("https://purl.imsglobal.org/spec/lti/claim/roles",
                            List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor"))
                    .build();

            User createdUser = User.builder()
                    .id(UUID.randomUUID())
                    .email(newEmail)
                    .moodleSub(newSub)
                    .role(UserRole.TEACHER)
                    .active(true)
                    .verified(true)
                    .build();

            when(userRepository.findByMoodleSub(newSub)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-pw");
            when(userRepository.save(any(User.class))).thenReturn(createdUser);
            when(jwtService.generateToken(createdUser)).thenReturn("new-user-token");

            String token = service.resolveAndGenerateToken(claims);

            assertEquals("new-user-token", token);

            // Vérification que l'utilisateur créé a les bonnes propriétés
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, atLeastOnce()).save(userCaptor.capture());
            User savedUser = userCaptor.getAllValues().stream()
                    .filter(u -> newEmail.equals(u.getEmail()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(savedUser, "L'utilisateur doit avoir été sauvegardé");
            assertEquals(newSub, savedUser.getMoodleSub());
            assertEquals(UserRole.TEACHER, savedUser.getRole());
        }

        @Test
        @DisplayName("resolveAndGenerateToken — Étudiant Moodle (Learner) → crée un compte STUDENT")
        void newUserLearner_createsStudentAccount() throws Exception {
            String studentEmail = "student@moodle.local";
            String studentSub = "moodle-sub-student-42";

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(studentSub)
                    .claim("email", studentEmail)
                    .claim("given_name", "Alice")
                    .claim("family_name", "Dupont")
                    .claim("https://purl.imsglobal.org/spec/lti/claim/roles",
                            List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner"))
                    .build();

            User createdStudent = User.builder()
                    .id(UUID.randomUUID())
                    .email(studentEmail)
                    .moodleSub(studentSub)
                    .role(UserRole.STUDENT)
                    .active(true)
                    .verified(true)
                    .build();

            when(userRepository.findByMoodleSub(studentSub)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(studentEmail)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-pw");
            when(userRepository.save(any(User.class))).thenReturn(createdStudent);
            when(jwtService.generateToken(createdStudent)).thenReturn("student-token");

            String token = service.resolveAndGenerateToken(claims);

            assertEquals("student-token", token);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, atLeastOnce()).save(captor.capture());
            User saved = captor.getAllValues().stream()
                    .filter(u -> studentEmail.equals(u.getEmail()))
                    .findFirst().orElse(null);
            assertNotNull(saved);
            assertEquals(UserRole.STUDENT, saved.getRole());
        }

        @Test
        @DisplayName("resolveAndGenerateToken — Pas d'email dans le JWT → email synthétique @moodle.lti")
        void newUserWithoutEmail_usesSyntheticEmail() throws Exception {
            String subNoEmail = "moodle-sub-no-email";
            String syntheticEmail = subNoEmail + "@moodle.lti";

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subNoEmail)
                    // Pas de claim "email"
                    .claim("https://purl.imsglobal.org/spec/lti/claim/roles",
                            List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor"))
                    .build();

            User createdUser = User.builder()
                    .id(UUID.randomUUID())
                    .email(syntheticEmail)
                    .moodleSub(subNoEmail)
                    .role(UserRole.TEACHER)
                    .active(true)
                    .build();

            when(userRepository.findByMoodleSub(subNoEmail)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(syntheticEmail)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-pw");
            when(userRepository.save(any(User.class))).thenReturn(createdUser);
            when(jwtService.generateToken(createdUser)).thenReturn("anon-token");

            String token = service.resolveAndGenerateToken(claims);

            assertEquals("anon-token", token);
            verify(userRepository).findByEmail(syntheticEmail);
        }
    }
}
