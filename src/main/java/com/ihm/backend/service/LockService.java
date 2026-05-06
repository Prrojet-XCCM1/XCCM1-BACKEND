package com.ihm.backend.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

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
        return locks.get(granuleId);
    }

    public void releaseAllLocksForUser(String userEmail) {
        locks.entrySet().removeIf(entry -> entry.getValue().equals(userEmail));
    }
}
