package com.ihm.backend.entity;

import com.ihm.backend.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Inscription d'un étudiant à une classe de cours.
 * Un étudiant ne peut être inscrit qu'une seule fois à une même classe.
 * L'enseignant valide ou rejette l'inscription.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "class_enrollments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_class_enrollment_student",
        columnNames = {"student_id", "class_id"}
    )
)
public class ClassEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * L'étudiant inscrit (role STUDENT obligatoire)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /**
     * La classe à laquelle l'étudiant s'inscrit
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private CourseClass courseClass;

    /**
     * Statut de l'inscription: PENDING → APPROVED ou REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private LocalDateTime enrolledAt;

    /**
     * Date de validation ou de rejet par l'enseignant
     */
    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    /**
     * L'enseignant qui a validé ou rejeté la demande
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;
}
