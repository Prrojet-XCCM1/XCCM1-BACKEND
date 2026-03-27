package com.ihm.backend.entity;

import java.time.LocalDateTime;
import java.util.Map;

import com.ihm.backend.enums.CourseStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private String category;
    private String description;
    @Enumerated(value = EnumType.STRING)
    private CourseStatus status;
    @ManyToOne
    private User author;

    /**
     * Classe (salle) à laquelle ce cours appartient. Optionnel.
     * Un cours peut être standalone ou rattaché à une classe thématique.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = true)
    private CourseClass courseClass;

    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> content;
    private String coverImage;
    private String photoUrl;

    @Builder.Default
    private Long viewCount = 0L;
    
    @Builder.Default
    private Long likeCount = 0L;
    
    @Builder.Default
    private Long downloadCount = 0L;
}
