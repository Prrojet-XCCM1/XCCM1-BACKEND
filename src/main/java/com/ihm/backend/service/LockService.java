package com.ihm.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LockService {

    // Format: lock:{courseId}:node:{nodeId}
    // Chaque nœud de chaque cours a son propre verrou Redis.
    private static final String LOCK_PREFIX = "lock:";
    // Les verrous expirent automatiquement après 8h pour éviter les verrous fantômes
    private static final long LOCK_TTL_HOURS = 8;

    private final StringRedisTemplate stringRedisTemplate;

    private String buildKey(Integer courseId, String nodeId) {
        return LOCK_PREFIX + courseId + ":node:" + nodeId;
    }

    /**
     * Acquérir le verrou sur un nœud spécifique d'un cours.
     * Retourne true si l'acquisition réussit, si l'utilisateur possède déjà le verrou,
     * ou si nodeId est null/vide/"0" (pas de verrou sur un nœud non identifié).
     */
    public boolean acquireLock(Integer courseId, String nodeId, String userEmail) {
        if (nodeId == null || nodeId.isEmpty() || nodeId.equals("0")) return true;
        String key = buildKey(courseId, nodeId);
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, userEmail, LOCK_TTL_HOURS, TimeUnit.HOURS);
        if (Boolean.TRUE.equals(acquired)) return true;
        // L'utilisateur possède déjà le verrou
        return userEmail.equals(stringRedisTemplate.opsForValue().get(key));
    }

    /**
     * Libérer le verrou sur un nœud. Seul le propriétaire du verrou peut le libérer.
     */
    public void releaseLock(Integer courseId, String nodeId, String userEmail) {
        if (nodeId == null || nodeId.isEmpty() || nodeId.equals("0")) return;
        String key = buildKey(courseId, nodeId);
        String owner = stringRedisTemplate.opsForValue().get(key);
        if (userEmail.equals(owner)) {
            stringRedisTemplate.delete(key);
        }
    }

    /**
     * Obtenir le propriétaire actuel du verrou d'un nœud.
     */
    public String getLockOwner(Integer courseId, String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) return null;
        return stringRedisTemplate.opsForValue().get(buildKey(courseId, nodeId));
    }

    /**
     * Libérer tous les verrous d'un utilisateur sur un cours donné.
     */
    public void releaseAllLocksForUser(Integer courseId, String userEmail) {
        String pattern = LOCK_PREFIX + courseId + ":node:*";
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys == null) return;
        for (String key : keys) {
            String owner = stringRedisTemplate.opsForValue().get(key);
            if (userEmail.equals(owner)) {
                stringRedisTemplate.delete(key);
            }
        }
    }

    /**
     * Libérer tous les verrous d'un utilisateur sur tous les cours.
     * Appelé lors d'une déconnexion WebSocket totale.
     */
    public void releaseAllLocksForUser(String userEmail) {
        Set<String> keys = stringRedisTemplate.keys(LOCK_PREFIX + "*");
        if (keys == null) return;
        for (String key : keys) {
            String owner = stringRedisTemplate.opsForValue().get(key);
            if (userEmail.equals(owner)) {
                stringRedisTemplate.delete(key);
            }
        }
    }
}
