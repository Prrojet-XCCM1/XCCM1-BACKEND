package cm.enspy.xccm.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    private UUID id;

    private String title;
    private String description;

    // Référence à la Composition
    private UUID compositionId;

    // Ordre d'affichage
    @Builder.Default
    private Integer orderIndex = 0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}