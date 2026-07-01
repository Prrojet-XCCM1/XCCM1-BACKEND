package com.ihm.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LockService {

    private static final String LOCK_PREFIX = "lock:granule:";
    // Les verrous expirent automatiquement après 8h pour éviter les verrous fantômes
    private static final long LOCK_TTL_HOURS = 8;

    private final StringRedisTemplate stringRedisTemplate;

    public boolean acquireLock(String granuleId, String userEmail) {
        if (granuleId == null) return true;
        String key = LOCK_PREFIX + granuleId;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, userEmail, LOCK_TTL_HOURS, TimeUnit.HOURS);
        if (Boolean.TRUE.equals(acquired)) return true;
        // L'utilisateur possède déjà le verrou
        return userEmail.equals(stringRedisTemplate.opsForValue().get(key));
    }

    public void releaseLock(String granuleId, String userEmail) {
        if (granuleId == null) return;
        String key = LOCK_PREFIX + granuleId;
        String owner = stringRedisTemplate.opsForValue().get(key);
        if (userEmail.equals(owner)) {
            stringRedisTemplate.delete(key);
        }
    }

    public String getLockOwner(String granuleId) {
        if (granuleId == null) return null;
        return stringRedisTemplate.opsForValue().get(LOCK_PREFIX + granuleId);
    }

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
