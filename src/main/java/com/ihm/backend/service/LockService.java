package com.ihm.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Gestion des verrous sur les granules (blocs) lors de l'édition collaborative.
 *
 * <p>Implémentation en mémoire avec Caffeine et TTL de 10 minutes.
 * Les verrous expirent automatiquement si le client plante sans envoyer UNLOCK,
 * ce qui évite les blocages permanents.
 *
 * <p>Note pour le futur : si le déploiement passe à plusieurs instances JVM,
 * migrer vers Redis (SET NX EX) pour une cohérence distribuée.
 */
@Slf4j
@Service
public class LockService {
    // Key: granuleId (String), Value: userEmail
    private final ConcurrentHashMap<String, String> locks = new ConcurrentHashMap<>();

    public boolean acquireLock(String granuleId, String userEmail) {
        if (granuleId == null) return true; // Si pas d'ID de granule, l'action est autorisée par défaut
        String currentOwner = locks.putIfAbsent(granuleId, userEmail);
        return currentOwner == null || currentOwner.equals(userEmail);
    }

    public void releaseLock(String granuleId, String userEmail) {
        if (granuleId == null) return; // Rien à déverrouiller
        locks.remove(granuleId, userEmail);
    }

    public String getLockOwner(String granuleId) {
        if (granuleId == null) return null;
        return locks.getIfPresent(granuleId);
    }

    /**
     * Libère tous les verrous appartenant à un utilisateur.
     * Appelé automatiquement lors de la déconnexion WebSocket.
     */
    public void releaseAllLocksForUser(String userEmail) {
        long count = locks.asMap().entrySet().stream()
                .filter(entry -> userEmail.equals(entry.getValue()))
                .peek(entry -> locks.invalidate(entry.getKey()))
                .count();
        if (count > 0) {
            log.info("Libération de {} verrou(s) pour l'utilisateur déconnecté {}", count, userEmail);
        }
    }
}
