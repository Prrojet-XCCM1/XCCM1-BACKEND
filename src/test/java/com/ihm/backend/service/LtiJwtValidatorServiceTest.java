package com.ihm.backend.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class LtiJwtValidatorServiceTest {

    private LtiJwtValidatorService validatorService;

    @Mock
    private LtiJwksService ltiJwksService;

    private RSAKey rsaKey;
    private JWKSet jwkSet;
    private static final String ISSUER = "https://moodle.example.com";
    private static final String AUDIENCE = "client-id-123";
    private static final String KID = "test-key-id";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        validatorService = new LtiJwtValidatorService(ltiJwksService);

        ReflectionTestUtils.setField(validatorService, "expectedIssuer", ISSUER);
        ReflectionTestUtils.setField(validatorService, "expectedAudience", AUDIENCE);

        // Generate a real RSA key pair for signing/verifying in tests
        rsaKey = new RSAKeyGenerator(2048)
                .keyID(KID)
                .generate();
        jwkSet = new JWKSet(rsaKey.toPublicJWK());

        when(ltiJwksService.getMoodleJwkSet()).thenReturn(jwkSet);
    }

    private String createToken(String iss, String aud, Date exp, String nonce) throws Exception {
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("user123")
                .issuer(iss)
                .audience(aud)
                .expirationTime(exp)
                .claim("nonce", nonce)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KID).build(),
                claimsSet);

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    @Test
    void shouldValidateCorrectToken() throws Exception {
        String token = createToken(ISSUER, AUDIENCE, new Date(System.currentTimeMillis() + 3600000), UUID.randomUUID().toString());
        
        JWTClaimsSet claims = validatorService.validateLtiJwt(token);
        
        assertNotNull(claims);
        assertEquals("user123", claims.getSubject());
    }

    @Test
    void shouldFailIfIssuerInvalid() throws Exception {
        String token = createToken("https://wrong-moodle.com", AUDIENCE, new Date(System.currentTimeMillis() + 3600000), UUID.randomUUID().toString());
        
        Exception exception = assertThrows(Exception.class, () -> validatorService.validateLtiJwt(token));
        assertTrue(exception.getMessage().contains("Issuer (iss) invalide"));
    }

    @Test
    void shouldFailIfAudienceInvalid() throws Exception {
        String token = createToken(ISSUER, "wrong-client", new Date(System.currentTimeMillis() + 3600000), UUID.randomUUID().toString());
        
        Exception exception = assertThrows(Exception.class, () -> validatorService.validateLtiJwt(token));
        assertTrue(exception.getMessage().contains("Audience (aud) invalide"));
    }

    @Test
    void shouldFailIfExpired() throws Exception {
        String token = createToken(ISSUER, AUDIENCE, new Date(System.currentTimeMillis() - 3600000), UUID.randomUUID().toString());
        
        Exception exception = assertThrows(Exception.class, () -> validatorService.validateLtiJwt(token));
        assertTrue(exception.getMessage().contains("Le token LTI a expiré"));
    }

    @Test
    void shouldFailIfNonceReused() throws Exception {
        String nonce = "reuse-me";
        String token1 = createToken(ISSUER, AUDIENCE, new Date(System.currentTimeMillis() + 3600000), nonce);
        String token2 = createToken(ISSUER, AUDIENCE, new Date(System.currentTimeMillis() + 3600000), nonce);
        
        validatorService.validateLtiJwt(token1);
        
        Exception exception = assertThrows(Exception.class, () -> validatorService.validateLtiJwt(token2));
        assertTrue(exception.getMessage().contains("Nonce déjà utilisé"));
    }

    @Test
    void shouldFailIfNoKidInHeader() throws Exception {
        JWSSigner signer = new RSASSASigner(rsaKey);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().issuer(ISSUER).audience(AUDIENCE).build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), claimsSet);
        signedJWT.sign(signer);
        String token = signedJWT.serialize();

        Exception exception = assertThrows(Exception.class, () -> validatorService.validateLtiJwt(token));
        assertTrue(exception.getMessage().contains("ne contient pas de Key ID"));
    }
}
