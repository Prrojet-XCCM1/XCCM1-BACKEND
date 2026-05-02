package com.ihm.backend.security.oauth2;

import com.ihm.backend.entity.User;
import com.ihm.backend.enums.AuthProvider;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oAuth2User.getAttributes());

        if (!StringUtils.hasText(userInfo.getEmail())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Email introuvable chez le fournisseur " + registrationId +
                    ". Activez la visibilité de votre email.");
        }

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        User user = userRepository.findByEmail(userInfo.getEmail())
                .map(existing -> updateExistingUser(existing, userInfo, provider))
                .orElseGet(() -> createOAuth2User(userInfo, provider));

        // Expose userId via attributes pour le success handler
        Map<String, Object> enrichedAttributes = new HashMap<>(oAuth2User.getAttributes());
        enrichedAttributes.put("user_id", user.getId().toString());
        enrichedAttributes.put("email", userInfo.getEmail());

        return new DefaultOAuth2User(user.getAuthorities(), enrichedAttributes, "email");
    }

    private User createOAuth2User(OAuth2UserInfo userInfo, AuthProvider provider) {
        String[] nameParts = splitName(userInfo.getName());

        User user = User.builder()
                .email(userInfo.getEmail())
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .photoUrl(userInfo.getImageUrl())
                .role(UserRole.STUDENT)
                .provider(provider)
                .providerId(userInfo.getId())
                // Mot de passe aléatoire non utilisable pour les comptes OAuth2
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .registrationDate(LocalDateTime.now())
                .active(true)
                .verified(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Nouveau compte OAuth2 créé : {} via {}", saved.getEmail(), provider);
        return saved;
    }

    private User updateExistingUser(User existing, OAuth2UserInfo userInfo, AuthProvider provider) {
        boolean updated = false;

        if (!StringUtils.hasText(existing.getPhotoUrl()) && StringUtils.hasText(userInfo.getImageUrl())) {
            existing.setPhotoUrl(userInfo.getImageUrl());
            updated = true;
        }

        // Si le compte existant est LOCAL, on lie le provider OAuth2
        if (existing.getProvider() == AuthProvider.LOCAL) {
            existing.setProvider(provider);
            existing.setProviderId(userInfo.getId());
            updated = true;
        }

        if (updated) {
            userRepository.save(existing);
            log.info("Compte OAuth2 lié à {} : {}", provider, existing.getEmail());
        }

        return existing;
    }

    private String[] splitName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            return new String[]{"Utilisateur", "OAuth2"};
        }
        int spaceIdx = fullName.indexOf(' ');
        if (spaceIdx == -1) {
            return new String[]{fullName, ""};
        }
        return new String[]{fullName.substring(0, spaceIdx), fullName.substring(spaceIdx + 1)};
    }
}
