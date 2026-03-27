package com.ihm.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Représente la première vue d'un utilisateur authentifié sur un cours.
 * Une seule vue est enregistrée par (utilisateur, cours).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "course_views",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "user_id"})
)
public class CourseView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "viewed_at", updatable = false)
    private LocalDateTime viewedAt;
}
