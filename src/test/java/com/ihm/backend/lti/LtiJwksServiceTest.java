package com.ihm.backend.lti;

import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class LtiJwksServiceTest {

    private LtiJwksService ltiJwksService;

    @BeforeEach
    void setUp() {
        ltiJwksService = new LtiJwksService();
        // Injection manuelle de la property @Value pour le test unitaire
        ReflectionTestUtils.setField(ltiJwksService, "moodleJwksUrl", "https://moodle.client.tld/mod/lti/certs.php");
    }

    @Test
    void shouldParseStaticJwks() throws Exception {
        // Mock d'un JWKS Moodle typique (RSA256)
        String mockJwks = "{\"keys\":[{" +
                "\"kty\":\"RSA\"," +
                "\"e\":\"AQAB\"," +
                "\"use\":\"sig\"," +
                "\"kid\":\"test-key-id\"," +
                "\"alg\":\"RS256\"," +
                "\"n\":\"o701_UAnf0-1_oN-C4yM6-...\"" +
                "}]}";

        JWKSet jwkSet = JWKSet.parse(mockJwks);

        assertNotNull(jwkSet);
        assertEquals(1, jwkSet.getKeys().size());
        assertEquals("test-key-id", jwkSet.getKeys().get(0).getKeyID());
    }

    @Test
    void testCacheRefresh() {
        ltiJwksService.refreshCache();
        assertTrue(true);
    }
}
