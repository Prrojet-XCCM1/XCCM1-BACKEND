package cm.enspy.xccm.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("password_reset_tokens")
public class PasswordResetToken {
    
    @Id
    private UUID id;
    
    @Column("token")
    private String token;
    
    @Column("user_id")
    private UUID userId;
    
    @Column("expiry_date")
    private LocalDateTime expiryDate;
    
    @Column("used")
    private Boolean used;
}