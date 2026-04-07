package com.ihm.backend.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LockService {
    // Key: granuleId, Value: userEmail
    private final ConcurrentHashMap<Long, String> locks = new ConcurrentHashMap<>();

    public boolean acquireLock(Long granuleId, String userEmail) {
        String currentOwner = locks.putIfAbsent(granuleId, userEmail);
        return currentOwner == null || currentOwner.equals(userEmail);
    }

    public void releaseLock(Long granuleId, String userEmail) {
        locks.remove(granuleId, userEmail);
    }

    public String getLockOwner(Long granuleId) {
        return locks.get(granuleId);
    }

    public void releaseAllLocksForUser(String userEmail) {
        locks.entrySet().removeIf(entry -> entry.getValue().equals(userEmail));
    }
}
