package com.ihm.backend.controller;
 
import com.ihm.backend.service.LtiOidcStateService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour LtiLoginController.
 * Couvre le flux OIDC Login Initiation LTI 1.3 (GET /lti/login).
 */
@ExtendWith(MockitoExtension.class)
class LtiLoginControllerTest {

    @Mock
    private LtiOidcStateService oidcStateService;

    @InjectMocks
    private LtiLoginController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "moodleUrl", "http://moodle.test.local");
        ReflectionTestUtils.setField(controller, "clientId", "xccm1-client-id");
        ReflectionTestUtils.setField(controller, "xccm1Issuer", "http://localhost:8082");
    }

    // =========================================================================
    // LOGIN INITIATION — CAS NOMINAL
    // =========================================================================

    @Nested
    @DisplayName("Login Initiation — Cas Nominal")
    class NominalCases {

        @Test
        @DisplayName("GET /lti/login - Redirection vers Moodle avec state et nonce")
        void login_validParams_redirectsToMoodle() throws Exception {
            when(oidcStateService.registerState()).thenReturn("test-state-uuid");

            MockHttpServletResponse response = new MockHttpServletResponse();
            controller.login(
                    "http://moodle.test.local",
                    "user123",
                    "http://localhost:8082/lti/launch",
                    null,
                    response);

            assertEquals(302, response.getStatus());
            String location = response.getRedirectedUrl();
            assertNotNull(location);
            assertTrue(location.contains("/mod/lti/auth.php"), "Doit rediriger vers l'auth endpoint Moodle");
            assertTrue(location.contains("client_id="), "URL doit contenir client_id");
            assertTrue(location.contains("state=test-state-uuid"), "URL doit contenir le state OIDC");
            assertTrue(location.contains("nonce="), "URL doit contenir un nonce");
            verify(oidcStateService).registerState();
        }

        @Test
        @DisplayName("GET /lti/login - Redirection avec lti_message_hint facultatif")
        void login_withLtiMessageHint_includesInRedirect() throws Exception {
            when(oidcStateService.registerState()).thenReturn("state-xyz");

            MockHttpServletResponse response = new MockHttpServletResponse();
            controller.login(
                    "http://moodle.test.local",
                    "student42",
                    "http://localhost:8082/lti/launch",
                    "msg-hint-abc",
                    response);

            assertEquals(302, response.getStatus());
            String location = response.getRedirectedUrl();
            assertTrue(location.contains("lti_message_hint="), "URL doit contenir lti_message_hint");
        }

        @Test
        @DisplayName("GET /lti/login - URL Moodle avec slash final ignoré")
        void login_moodleUrlWithTrailingSlash_normalizes() throws Exception {
            // Moodle envoie parfois l'issuer avec un slash final
            ReflectionTestUtils.setField(controller, "moodleUrl", "http://moodle.test.local/");
            when(oidcStateService.registerState()).thenReturn("state-abc");

            MockHttpServletResponse response = new MockHttpServletResponse();
            controller.login(
                    "http://moodle.test.local",
                    "user1",
                    "http://localhost:8082/lti/launch",
                    null,
                    response);

            assertEquals(302, response.getStatus());
        }
    }

    // =========================================================================
    // LOGIN INITIATION — CAS D'ERREUR
    // =========================================================================

    @Nested
    @DisplayName("Login Initiation — Cas d'Erreur")
    class ErrorCases {

        @Test
        @DisplayName("GET /lti/login - Issuer inconnu → 400 Bad Request")
        void login_unknownIssuer_returns400() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            controller.login(
                    "http://unknown-lms.com",
                    "user456",
                    "http://localhost:8082/lti/launch",
                    null,
                    response);

            assertEquals(400, response.getStatus());
            verifyNoInteractions(oidcStateService);
        }

        @Test
        @DisplayName("GET /lti/login - target_link_uri invalide → 400 Bad Request")
        void login_invalidTargetLinkUri_returns400() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            controller.login(
                    "http://moodle.test.local",
                    "user789",
                    "http://evil.example.com/lti/launch",
                    null,
                    response);

            assertEquals(400, response.getStatus());
            verifyNoInteractions(oidcStateService);
        }

        @Test
        @DisplayName("GET /lti/login - Issuer null → 400 Bad Request")
        void login_nullIssuer_returns400() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            controller.login(
                    null,
                    "user999",
                    "http://localhost:8082/lti/launch",
                    null,
                    response);

            assertEquals(400, response.getStatus());
        }
    }
}
