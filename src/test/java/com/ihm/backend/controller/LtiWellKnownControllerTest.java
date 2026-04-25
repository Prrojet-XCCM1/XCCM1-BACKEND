package com.ihm.backend.controller;
 
import com.ihm.backend.service.LtiKeyService;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires pour LtiWellKnownController.
 * Couvre l'exposition de la clé publique RSA au format JWKS (GET
 * /lti/.well-known/jwks.json).
 */
@ExtendWith(MockitoExtension.class)
class LtiWellKnownControllerTest {

    @Mock
    private LtiKeyService ltiKeyService;

    @InjectMocks
    private LtiWellKnownController controller;

    @Test
    @DisplayName("GET /lti/.well-known/jwks.json - Retourne un JWKS valide avec la clé publique")
    void getJwks_returnsValidJwkSet() throws Exception {
        // Génère une paire RSA de test
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var kp = kpg.generateKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                .privateKey((RSAPrivateKey) kp.getPrivate())
                .keyID("test-kid-1")
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey.toPublicJWK());

        when(ltiKeyService.getPublicJwkSet()).thenReturn(jwkSet);

        Map<String, Object> result = controller.getJwks();

        assertNotNull(result, "Le résultat JWKS ne doit pas être null");
        assertTrue(result.containsKey("keys"), "Le JWKS doit contenir une entrée 'keys'");

        @SuppressWarnings("unchecked")
        var keys = (java.util.List<Map<String, Object>>) result.get("keys");
        assertFalse(keys.isEmpty(), "La liste des clés ne doit pas être vide");
        assertEquals("RSA", keys.get(0).get("kty"), "Le type de clé doit être RSA");
        assertEquals("test-kid-1", keys.get(0).get("kid"), "Le kid doit correspondre");
    }

    @Test
    @DisplayName("GET /lti/.well-known/jwks.json - La clé publique ne contient pas la clé privée")
    void getJwks_doesNotExposePrivateKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var kp = kpg.generateKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                .privateKey((RSAPrivateKey) kp.getPrivate())
                .keyID("test-kid-public-only")
                .build();
        // Seule la clé PUBLIQUE est exposée
        JWKSet publicJwkSet = new JWKSet(rsaKey.toPublicJWK());

        when(ltiKeyService.getPublicJwkSet()).thenReturn(publicJwkSet);

        Map<String, Object> result = controller.getJwks();

        @SuppressWarnings("unchecked")
        var keys = (java.util.List<Map<String, Object>>) result.get("keys");
        Map<String, Object> key = keys.get(0);

        // La clé privée (d) ne doit PAS être présente dans le JWKS public
        assertFalse(key.containsKey("d"), "La clé privée RSA 'd' ne doit pas être exposée dans /jwks.json");
    }
}
