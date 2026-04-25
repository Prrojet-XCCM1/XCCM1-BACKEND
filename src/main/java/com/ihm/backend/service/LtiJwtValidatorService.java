package com.ihm.backend.service;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LtiJwtValidatorService {

    private final LtiJwksService ltiJwksService;

    @Value("${lti.moodle.platform-url}")
    private String expectedIssuer;

    @Value("${lti.moodle.client-id}")
    private String expectedAudience;

    // Cache local pour les nonces afin d'éviter les attaques par rejeu.
    // Dans une architecture multi-instance, préférez Redis.
    private final Map<String, Instant> usedNonces = new ConcurrentHashMap<>();

    /**
     * Valide mathématiquement et sémantiquement le token envoyé par Moodle.
     * 
     * @param idToken Le JWT brut envoyé par Moodle.
     * @return Les claims extraits si le token est valide.
     * @throws Exception Si une validation échoue.
     */
    public JWTClaimsSet validateLtiJwt(String idToken) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(idToken);

        // 1. Récupération du Key ID (kid) pour trouver la bonne clé publique
        String kid = signedJWT.getHeader().getKeyID();
        if (kid == null) {
            throw new Exception("Le Header JWT ne contient pas de Key ID (kid)");
        }

        // 2. Récupération du JWKS Moodle et extraction de la clé RSA
        JWKSet jwkSet = ltiJwksService.getMoodleJwkSet();
        RSAKey rsaKey = (RSAKey) jwkSet.getKeyByKeyId(kid);
        if (rsaKey == null) {
            log.error("Clé publique introuvable pour kid={}. JWKS dispo : {}", kid, jwkSet.toJSONObject());
            throw new Exception("Clé Moodle introuvable pour kid=" + kid);
        }

        // 3. Vérification de la signature RSA
        JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
        if (!signedJWT.verify(verifier)) {
            throw new Exception("Signature RSA invalide");
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        // 4. Validation des claims obligatoires LTI 1.3
        validateClaims(claims);

        log.info("JWT LTI validé avec succès pour sub : {}", claims.getSubject());
        return claims;
    }

    private void validateClaims(JWTClaimsSet claims) throws Exception {
        // Validation de l'émetteur (iss)
        if (!expectedIssuer.equals(claims.getIssuer())) {
            throw new Exception("Issuer (iss) invalide. Attendu=" + expectedIssuer + ", Reçu=" + claims.getIssuer());
        }

        // Validation de l'audience (aud)
        if (claims.getAudience() == null || !claims.getAudience().contains(expectedAudience)) {
            throw new Exception(
                    "Audience (aud) invalide. Attendu=" + expectedAudience + ", Reçu=" + claims.getAudience());
        }

        // Validation de l'expiration (exp)
        Date expirationTime = claims.getExpirationTime();
        if (expirationTime == null || expirationTime.before(new Date())) {
            throw new Exception("Le token LTI a expiré");
        }

        // Validation du nonce (anti-replay)
        String nonce = (String) claims.getClaim("nonce");
        if (nonce == null || nonce.isBlank()) {
            throw new Exception("Claim 'nonce' manquant");
        }
        if (usedNonces.containsKey(nonce)) {
            throw new Exception("Nonce déjà utilisé (attaque par rejeu potentielle)");
        }

        // Nettoyage régulier du cache des nonces
        cleanNonceCache();
        usedNonces.put(nonce, Instant.now());
    }

    private void cleanNonceCache() {
        // Supprime les nonces vieux de plus d'une heure
        Instant threshold = Instant.now().minusSeconds(3600);
        usedNonces.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }
}
