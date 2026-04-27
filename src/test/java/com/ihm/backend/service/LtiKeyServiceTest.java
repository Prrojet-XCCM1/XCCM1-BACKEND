package com.ihm.backend.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LtiKeyServiceTest {

    @Test
    void shouldGenerateKeyWhenNoPemPathProvided() throws Exception {
        LtiKeyService ltiKeyService = new LtiKeyService("", "test-kid");
        
        RSAKey rsaKey = ltiKeyService.getRsaKey();
        assertNotNull(rsaKey);
        assertTrue(rsaKey.getKeyID().startsWith("test-kid-"));
        assertNotNull(rsaKey.toRSAPublicKey());
        assertNotNull(rsaKey.toRSAPrivateKey());
    }

    @Test
    void shouldReturnPublicJwkSet() throws Exception {
        LtiKeyService ltiKeyService = new LtiKeyService(null, "test-kid");
        
        JWKSet jwkSet = ltiKeyService.getPublicJwkSet();
        assertNotNull(jwkSet);
        assertEquals(1, jwkSet.getKeys().size());
        assertFalse(jwkSet.getKeys().get(0).isPrivate());
    }
}
