package cm.enspy.xccm.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import cm.enspy.xccm.domain.enums.GranuleType;

@Table("granules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Granule {

    @Id
    private UUID id;

    private String title;
    private String content; // Contenu texte ou JSON

    // Type de granule
    @Builder.Default
    private GranuleType type = GranuleType.TEXT;

    // Référence à la Section
    private UUID sectionId;

    // Ordre d'affichage
    @Builder.Default
    private Integer orderIndex = 0;

    // Réutilisabilité
    @Builder.Default
    private Boolean reusable = false;

    // Tags (stockés en JSON ou table séparée)
    private String tags;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
