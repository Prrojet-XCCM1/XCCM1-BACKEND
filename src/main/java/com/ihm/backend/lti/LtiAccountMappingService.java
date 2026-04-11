package com.ihm.backend.lti;

import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.repository.jpa.UserRepository;
import com.ihm.backend.service.JwtService;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LtiAccountMappingService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Résout un utilisateur XCCM1 à partir des claims Moodle.
     * Crée le compte s'il n'existe pas (JIT Provisioning).
     *
     * @param claims Les claims extraits et validés du token Moodle.
     * @return Le token JWT interne à XCCM1 pour cet utilisateur.
     */
    @Transactional
    public String resolveAndGenerateToken(JWTClaimsSet claims) {
        String moodleSub = claims.getSubject();
        String email = (String) claims.getClaim("email");
        String firstName = (String) claims.getClaim("given_name");
        String lastName = (String) claims.getClaim("family_name");
        UserRole ltiRole = LtiRoleMapper.resolveRole(claims);

        log.info("Résolution de l'utilisateur pour moodle_sub: {}", moodleSub);

        User user = userRepository.findByMoodleSub(moodleSub)
                .orElseGet(() -> provisionOrCreateUser(moodleSub, email, firstName, lastName, ltiRole));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return jwtService.generateToken(user);
    }

    private User provisionOrCreateUser(
            String moodleSub,
            String email,
            String firstName,
            String lastName,
            UserRole ltiRole) {

        log.info("Utilisateur inconnu pour moodle_sub={}, création ou liaison", moodleSub);

        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email)
                    .map(existingUser -> {
                        log.info("Liaison LTI : email {} → moodle_sub {}", email, moodleSub);
                        existingUser.setMoodleSub(moodleSub);
                        return userRepository.save(existingUser);
                    })
                    .orElseGet(() -> createNewUser(moodleSub, email, firstName, lastName, ltiRole));
        }

        String syntheticEmail = moodleSub + "@moodle.lti";
        return userRepository.findByEmail(syntheticEmail)
                .map(u -> {
                    u.setMoodleSub(moodleSub);
                    return userRepository.save(u);
                })
                .orElseGet(() -> createNewUser(moodleSub, syntheticEmail, firstName, lastName, ltiRole));
    }

    private User createNewUser(
            String moodleSub,
            String email,
            String firstName,
            String lastName,
            UserRole ltiRole) {

        log.info("Nouvel utilisateur JIT LTI : {}", email);
        User newUser = User.builder()
                .email(email)
                .moodleSub(moodleSub)
                .firstName(firstName != null ? firstName : "Moodle")
                .lastName(lastName != null ? lastName : "User")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(ltiRole)
                .registrationDate(LocalDateTime.now())
                .active(true)
                .verified(true)
                .build();
        return userRepository.save(newUser);
    }
}
