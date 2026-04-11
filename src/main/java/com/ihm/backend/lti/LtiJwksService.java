package com.ihm.backend.lti;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class LtiJwksService {

    @Value("${lti.moodle.jwks-url}")
    private String moodleJwksUrl;

    private JWKSet cachedJwkSet;
    private Instant cacheTime;

    /**
     * Récupère le JWKSet de Moodle avec un système de cache simple (1 heure).
     */
    public JWKSet getMoodleJwkSet() throws Exception {
        if (cachedJwkSet == null || cacheTime == null || Duration.between(cacheTime, Instant.now()).toHours() >= 1) {
            log.info("Récupération du JWKS Moodle depuis : {}", moodleJwksUrl);
            try {
                cachedJwkSet = JWKSet.load(new URL(moodleJwksUrl));
                cacheTime = Instant.now();
                log.info("JWKS Moodle récupéré avec succès ({} clés trouvées)", cachedJwkSet.getKeys().size());
            } catch (Exception e) {
                log.error("Erreur lors de la récupération du JWKS Moodle : {}", e.getMessage());
                if (cachedJwkSet != null) {
                    log.warn("Utilisation du JWKS en cache malgré l'erreur de rafraîchissement.");
                    return cachedJwkSet;
                }
                throw e;
            }
        }
        return cachedJwkSet;
    }

    /**
     * Force le rafraîchissement du cache (utile en cas de rotation de clés).
     */
    public void refreshCache() {
        this.cacheTime = null;
    }
}
