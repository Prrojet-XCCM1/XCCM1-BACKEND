package cm.enspy.xccm.repository;

import cm.enspy.xccm.domain.entity.PasswordResetToken;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PasswordResetTokenRepository extends R2dbcRepository<PasswordResetToken, UUID> {
    Mono<PasswordResetToken> findByToken(String token);
    
    @Query("UPDATE password_reset_tokens SET used = true WHERE user_id = :userId")
    Mono<Void> markAllTokensAsUsed(UUID userId);
    
    @Query("DELETE FROM password_reset_tokens WHERE expiry_date < :date")
    Mono<Void> deleteAllExpiredSince(LocalDateTime date);
}