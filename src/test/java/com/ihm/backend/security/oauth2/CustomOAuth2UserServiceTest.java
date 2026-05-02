package com.ihm.backend.security.oauth2;

import com.ihm.backend.repository.jpa.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomOAuth2UserService — Tests unitaires")
class CustomOAuth2UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    // =========================================================================
    //  OAuth2UserInfoFactory
    // =========================================================================
    @Nested
    @DisplayName("OAuth2UserInfoFactory — Création d'info utilisateur")
    class UserInfoFactory {

        @Test
        @DisplayName("✅ Google → GoogleOAuth2UserInfo")
        void google_returnsGoogleInfo() {
            Map<String, Object> attrs = Map.of(
                    "sub", "google-123",
                    "name", "Jean Dupont",
                    "email", "jean@gmail.com",
                    "picture", "https://photo.url");

            OAuth2UserInfo info = OAuth2UserInfoFactory.getOAuth2UserInfo("google", attrs);

            assertThat(info).isInstanceOf(GoogleOAuth2UserInfo.class);
            assertThat(info.getId()).isEqualTo("google-123");
            assertThat(info.getEmail()).isEqualTo("jean@gmail.com");
            assertThat(info.getName()).isEqualTo("Jean Dupont");
            assertThat(info.getImageUrl()).isEqualTo("https://photo.url");
        }

        @Test
        @DisplayName("✅ GitHub → GithubOAuth2UserInfo")
        void github_returnsGithubInfo() {
            Map<String, Object> attrs = Map.of(
                    "id", 456,
                    "name", "Marie Curie",
                    "email", "marie@github.com",
                    "avatar_url", "https://avatar.url");

            OAuth2UserInfo info = OAuth2UserInfoFactory.getOAuth2UserInfo("github", attrs);

            assertThat(info).isInstanceOf(GithubOAuth2UserInfo.class);
            assertThat(info.getId()).isEqualTo("456");
            assertThat(info.getEmail()).isEqualTo("marie@github.com");
            assertThat(info.getImageUrl()).isEqualTo("https://avatar.url");
        }

        @Test
        @DisplayName("✅ GitHub sans nom → fallback sur login")
        void github_noName_usesLogin() {
            Map<String, Object> attrs = Map.of(
                    "id", 789,
                    "login", "dev_user",
                    "email", "dev@github.com");

            OAuth2UserInfo info = OAuth2UserInfoFactory.getOAuth2UserInfo("github", attrs);

            assertThat(info.getName()).isEqualTo("dev_user");
        }

        @Test
        @DisplayName("✅ Provider inconnu → OAuth2AuthenticationException")
        void unknownProvider_throwsException() {
            assertThatThrownBy(() ->
                    OAuth2UserInfoFactory.getOAuth2UserInfo("facebook", Map.of()))
                    .isInstanceOf(OAuth2AuthenticationException.class);
        }
    }

    // =========================================================================
    //  GoogleOAuth2UserInfo
    // =========================================================================
    @Nested
    @DisplayName("GoogleOAuth2UserInfo — Extraction des attributs")
    class GoogleUserInfo {

        @Test
        @DisplayName("✅ Tous les champs Google correctement extraits")
        void allFields_extracted() {
            Map<String, Object> attrs = Map.of(
                    "sub", "g-id-123",
                    "name", "Alice Martin",
                    "email", "alice@gmail.com",
                    "picture", "https://pic.google.com/photo.jpg");

            GoogleOAuth2UserInfo info = new GoogleOAuth2UserInfo(attrs);

            assertThat(info.getId()).isEqualTo("g-id-123");
            assertThat(info.getName()).isEqualTo("Alice Martin");
            assertThat(info.getEmail()).isEqualTo("alice@gmail.com");
            assertThat(info.getImageUrl()).isEqualTo("https://pic.google.com/photo.jpg");
        }

        @Test
        @DisplayName("✅ Champs manquants → null retourné sans exception")
        void missingFields_returnsNull() {
            GoogleOAuth2UserInfo info = new GoogleOAuth2UserInfo(Map.of());

            assertThat(info.getId()).isNull();
            assertThat(info.getName()).isNull();
            assertThat(info.getEmail()).isNull();
            assertThat(info.getImageUrl()).isNull();
        }
    }

    // =========================================================================
    //  GithubOAuth2UserInfo
    // =========================================================================
    @Nested
    @DisplayName("GithubOAuth2UserInfo — Extraction des attributs")
    class GithubUserInfo {

        @Test
        @DisplayName("✅ ID numérique converti en String")
        void numericId_convertedToString() {
            GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(Map.of("id", 12345));
            assertThat(info.getId()).isEqualTo("12345");
        }

        @Test
        @DisplayName("✅ ID absent → null")
        void missingId_returnsNull() {
            GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(Map.of());
            assertThat(info.getId()).isNull();
        }

        @Test
        @DisplayName("✅ Nom présent → utilise name")
        void hasName_usesName() {
            GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(
                    Map.of("name", "Bob", "login", "bob_login"));
            assertThat(info.getName()).isEqualTo("Bob");
        }

        @Test
        @DisplayName("✅ Nom absent → fallback sur login")
        void noName_fallsBackToLogin() {
            GithubOAuth2UserInfo info = new GithubOAuth2UserInfo(Map.of("login", "bob_login"));
            assertThat(info.getName()).isEqualTo("bob_login");
        }
    }
}
