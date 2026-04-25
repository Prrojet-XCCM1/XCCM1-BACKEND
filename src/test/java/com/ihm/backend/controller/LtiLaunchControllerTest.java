package com.ihm.backend.controller;
 
import com.ihm.backend.service.LtiAccountMappingService;
import com.ihm.backend.service.LtiJwtValidatorService;
import com.ihm.backend.service.LtiOidcStateService;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour LtiLaunchController.
 * Couvre le flux LTI 1.3 Launch (POST /lti/launch) :
 * - token manquant → 400
 * - state OIDC invalide → 400
 * - JWT invalide → 401
 * - lancement réussi → 200 + token XCCM1
 */
@ExtendWith(MockitoExtension.class)
class LtiLaunchControllerTest {

    @Mock
    private LtiJwtValidatorService ltiJwtValidatorService;

    @Mock
    private LtiAccountMappingService ltiAccountMappingService;

    @Mock
    private LtiOidcStateService ltiOidcStateService;

    @InjectMocks
    private LtiLaunchController controller;

    // =========================================================================
    // CAS NOMINAUX
    // =========================================================================

    @Nested
    @DisplayName("Lancement LTI réussi")
    class SuccessCases {

        @Test
        @DisplayName("POST /lti/launch - JWT valide → 200 avec token XCCM1")
        void launch_validToken_returns200WithToken() throws Exception {
            String validState = "valid-state-uuid";
            String idToken = "header.payload.signature";

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("moodle-user-1")
                    .issuer("http://moodle.test.local")
                    .audience("xccm1-client-id")
                    .expirationTime(new Date(System.currentTimeMillis() + 3600000))
                    .claim("email", "teacher@moodle.local")
                    .claim("name", "Marie Dupont")
                    .claim("nonce", "nonce-abc")
                    .build();

            doNothing().when(ltiOidcStateService).validateAndConsume(validState);
            when(ltiJwtValidatorService.validateLtiJwt(idToken)).thenReturn(claims);
            when(ltiAccountMappingService.resolveAndGenerateToken(claims)).thenReturn("xccm1-jwt-token-xyz");

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) controller
                    .launch(idToken, validState);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals("success", body.get("status"));
            assertEquals("xccm1-jwt-token-xyz", body.get("token"));
            assertNotNull(body.get("user"));
        }

        @Test
        @DisplayName("POST /lti/launch - Claim email absent → utilise le subject comme email")
        void launch_noEmailClaim_usesSubjectAsEmail() throws Exception {
            String idToken = "header.payload.sig";
            String state = "state-no-email";

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("moodle-sub-xyz")
                    .claim("nonce", "nonce-xyz")
                    .build();

            doNothing().when(ltiOidcStateService).validateAndConsume(state);
            when(ltiJwtValidatorService.validateLtiJwt(idToken)).thenReturn(claims);
            when(ltiAccountMappingService.resolveAndGenerateToken(claims)).thenReturn("token-for-anon");

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) controller
                    .launch(idToken, state);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // =========================================================================
    // CAS D'ERREUR
    // =========================================================================

    @Nested
    @DisplayName("Erreurs de lancement LTI")
    class ErrorCases {

        @Test
        @DisplayName("POST /lti/launch - id_token manquant → 400 Bad Request")
        void launch_missingIdToken_returns400() {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) controller.launch(null,
                    "some-state");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("id_token manquant", response.getBody().get("error"));
            verifyNoInteractions(ltiOidcStateService, ltiJwtValidatorService);
        }

        @Test
        @DisplayName("POST /lti/launch - id_token vide → 400 Bad Request")
        void launch_blankIdToken_returns400() {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) controller
                    .launch("   ", "some-state");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(ltiOidcStateService, ltiJwtValidatorService);
        }

        @Test
        @DisplayName("POST /lti/launch - State OIDC invalide → 400 Bad Request")
        void launch_invalidState_returns400() throws Exception {
            doThrow(new IllegalArgumentException("state OIDC invalide ou déjà utilisé"))
                    .when(ltiOidcStateService).validateAndConsume("bad-state");

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) controller
                    .launch("valid.jwt.token", "bad-state");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().get("error").toString().contains("state OIDC invalide"));
            verifyNoInteractions(ltiJwtValidatorService);
        }

        @Test
        @DisplayName("POST /lti/launch - JWT RSA invalide → 401 Unauthorized")
        void launch_invalidJwt_returns401() throws Exception {
            doNothing().when(ltiOidcStateService).validateAndConsume("valid-state");
            when(ltiJwtValidatorService.validateLtiJwt(anyString()))
                    .thenThrow(new Exception("Signature RSA invalide"));

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) controller
                    .launch("invalid.jwt.token", "valid-state");

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertTrue(response.getBody().get("error").toString().contains("Authentification LTI échouée"));
            verifyNoInteractions(ltiAccountMappingService);
        }

        @Test
        @DisplayName("POST /lti/launch - State null → 400 Bad Request")
        void launch_nullState_returns400() throws Exception {
            doThrow(new IllegalArgumentException("state OIDC manquant"))
                    .when(ltiOidcStateService).validateAndConsume(null);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) controller
                    .launch("some.jwt.token", null);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}
