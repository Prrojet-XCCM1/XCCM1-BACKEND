package cm.enspy.xccm.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import cm.enspy.xccm.domain.enums.CompositionStatus;

@Table("compositions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Composition {

    @Id
    private UUID id;

    private String title;
    private String description;

    // Référence au Teacher (relation via teacherId)
    private UUID teacherId;

    @Builder.Default
    private CompositionStatus status = CompositionStatus.DRAFT;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

}