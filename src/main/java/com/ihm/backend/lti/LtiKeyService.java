package com.ihm.backend.lti;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class LtiKeyService {

    private final RSAKey rsaKey;

    public LtiKeyService(
            @Value("${lti.tool.private-key-pem-path:}") String privateKeyPemPath,
            @Value("${lti.tool.key-id:xccm1-lti-key-1}") String configuredKeyId) {
        try {
            if (privateKeyPemPath != null && !privateKeyPemPath.isBlank()) {
                String pem = Files.readString(Path.of(privateKeyPemPath));
                this.rsaKey = loadFromPkcs8Pem(pem, configuredKeyId);
                log.info("Clé RSA LTI chargée depuis {} (kid={})", privateKeyPemPath, rsaKey.getKeyID());
            } else {
                log.warn(
                        "Aucun lti.tool.private-key-pem-path : génération d'une paire RSA au démarrage "
                                + "(kid change à chaque redémarrage — à éviter en prod / Moodle).");
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                var kp = kpg.generateKeyPair();
                this.rsaKey = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                        .privateKey((RSAPrivateKey) kp.getPrivate())
                        .keyID(configuredKeyId + "-" + UUID.randomUUID().toString().substring(0, 8))
                        .build();
                log.info("Clé RSA LTI générée (kid={})", rsaKey.getKeyID());
            }
        } catch (Exception e) {
            log.error("Initialisation des clés LTI impossible : {}", e.getMessage());
            throw new RuntimeException("Échec de l'initialisation des clés LTI", e);
        }
    }

    private static RSAKey loadFromPkcs8Pem(String pem, String keyId) throws Exception {
        String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(stripped);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKey priv = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        if (!(priv instanceof RSAPrivateCrtKey crt)) {
            throw new IllegalArgumentException("La clé PKCS#8 doit être une clé RSA CRT");
        }
        RSAPublicKey pub = (RSAPublicKey) kf.generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
        return new RSAKey.Builder(pub).privateKey(priv).keyID(keyId).build();
    }

    public JWKSet getPublicJwkSet() {
        return new JWKSet(rsaKey.toPublicJWK());
    }

    public RSAKey getRsaKey() {
        return rsaKey;
    }
}
