package com.ihm.backend.lti;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Valide le paramètre {@code state} du flux OIDC LTI (anti-CSRF / corrélation).
 * En multi-instances, remplacer par un store partagé (Redis).
 */
@Service
public class LtiOidcStateService {

    private static final long TTL_SECONDS = 600;

    private final Map<String, Instant> pendingStates = new ConcurrentHashMap<>();

    public String registerState() {
        cleanExpired();
        String state = UUID.randomUUID().toString();
        pendingStates.put(state, Instant.now());
        return state;
    }

    /**
     * @throws IllegalArgumentException si state absent, inconnu, expiré ou déjà consommé
     */
    public void validateAndConsume(String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("state OIDC manquant");
        }
        cleanExpired();
        Instant created = pendingStates.remove(state);
        if (created == null) {
            throw new IllegalArgumentException("state OIDC invalide ou déjà utilisé");
        }
    }

    private void cleanExpired() {
        Instant threshold = Instant.now().minusSeconds(TTL_SECONDS);
        pendingStates.entrySet().removeIf(e -> e.getValue().isBefore(threshold));
    }
}
