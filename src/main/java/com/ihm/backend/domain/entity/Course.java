package cm.enspy.xccm.domain.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    private UUID id;
    private String title;
    private String description;
    private String category;
    private String coverImageUrl;
    private UUID authorId;
    private UUID compositionId;
    private LocalDateTime datePublication;
    private int viewCount;
}