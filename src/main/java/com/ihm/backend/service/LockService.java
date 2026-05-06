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

    /**
     * Key: granuleId — Value: userEmail propriétaire du verrou.
     * TTL de 10 minutes : un verrou non libéré explicitement est automatiquement expiré.
     * Taille max 10 000 verrous simultanés.
     */
    private final Cache<Long, String> locks = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    /**
     * Tente d'acquérir le verrou sur un granule.
     *
     * @return true si le verrou est acquis (ou si l'utilisateur le possède déjà)
     */
    public boolean acquireLock(Long granuleId, String userEmail) {
        if (granuleId == null) {
            // Pas d'ID de granule → action autorisée par défaut (ex: curseur global)
            return true;
        }
        // putIfAbsent est atomique sur ConcurrentHashMap sous-jacent de Caffeine
        String existing = locks.asMap().putIfAbsent(granuleId, userEmail);
        boolean acquired = existing == null || existing.equals(userEmail);
        if (!acquired) {
            log.debug("Lock refusé sur granule {} pour {} (propriétaire actuel: {})", granuleId, userEmail, existing);
        }
        return acquired;
    }

    /**
     * Libère le verrou uniquement si l'utilisateur en est propriétaire.
     */
    public void releaseLock(Long granuleId, String userEmail) {
        if (granuleId == null) return;
        boolean removed = locks.asMap().remove(granuleId, userEmail);
        if (removed) {
            log.debug("Lock libéré sur granule {} par {}", granuleId, userEmail);
        }
    }

    /**
     * Retourne l'email du propriétaire actuel du verrou, ou null si non verrouillé / expiré.
     */
    public String getLockOwner(Long granuleId) {
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
