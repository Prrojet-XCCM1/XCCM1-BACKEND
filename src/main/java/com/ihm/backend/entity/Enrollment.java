package com.ihm.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // L'étudiant enrôlé (role doit être STUDENT)

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;  // Le cours

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "progress")
    @Builder.Default
    private Double progress = 0.0;  // Progression en % (0-100)

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "completed")
    private Boolean completed;

    // Méthodes utilitaires pour exposition via DTO
    @Transient
    public Integer getCourseId() {
        return course != null ? course.getId() : null;
    }

    @Transient
    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }
}
