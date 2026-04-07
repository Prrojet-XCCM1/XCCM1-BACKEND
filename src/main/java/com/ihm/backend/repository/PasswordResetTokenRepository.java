// PasswordResetTokenRepository.java
package com.ihm.backend.repository;

import com.ihm.backend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteAllByExpiryDateBefore(LocalDateTime dateTime);

    void deleteByUserId(UUID userId);
}