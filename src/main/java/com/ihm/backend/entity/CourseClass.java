package com.ihm.backend.entity;

import com.ihm.backend.enums.ClassStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente une salle/classe de cours créée par un enseignant.
 * Regroupe plusieurs cours d'une même thématique.
 * Les étudiants s'inscrivent à la classe (et non cours par cours).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_classes")
@Document(indexName = "course_classes")
public class CourseClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @org.springframework.data.annotation.Id
    private Long id;

    @Column(nullable = false)
    @Field(type = FieldType.Text, analyzer = "french")
    private String name;

    @Column(columnDefinition = "TEXT")
    @Field(type = FieldType.Text, analyzer = "french")
    private String description;

    /**
     * Thématique de la classe (ex: "Programmation", "Design", "Data Science")
     */
    @Field(type = FieldType.Keyword)
    private String theme;

    @Column(name = "cover_image")
    private String coverImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClassStatus status = ClassStatus.OPEN;

    /**
     * L'enseignant propriétaire de la classe
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    /**
     * Les cours appartenant à cette classe.
     * Un cours peut appartenir à une seule classe à la fois (nullable).
     */
    @OneToMany(mappedBy = "courseClass", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Course> courses = new ArrayList<>();

    /**
     * Nombre maximum d'étudiants (0 = illimité)
     */
    @Column(name = "max_students")
    @Builder.Default
    private Integer maxStudents = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
