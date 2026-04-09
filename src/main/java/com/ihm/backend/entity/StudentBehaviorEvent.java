package com.ihm.backend.entity;

import com.ihm.backend.enums.BehaviorEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Enregistre chaque interaction significative d'un étudiant avec le contenu.
 * Permet d'alimenter le moteur d'évaluation de niveau (LLM Service).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_behavior_events", indexes = {
    @Index(name = "idx_behavior_student", columnList = "student_id"),
    @Index(name = "idx_behavior_granule", columnList = "granule_id")
})
public class StudentBehaviorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /**
     * Type d'événement comportemental.
     * Ex: CONTENT_READ, EXERCISE_SUBMITTED, AI_QUESTION_ASKED, VIDEO_WATCHED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private BehaviorEventType eventType;

    /**
     * Identifiant du granule (contenu) concerné par l'événement.
     * Correspond à l'ID de la table `granules` dans le LLM Service.
     */
    @Column(name = "granule_id")
    private Integer granuleId;

    /**
     * Notion/sujet concerné (ex: "Programmation Orientée Objet", "Calcul Intégral").
     * Utilisé directement pour mettre à jour le StudentKnowledgeState.
     */
    @Column(name = "notion", nullable = false)
    private String notion;

    /**
     * Score brut pour les événements quantifiables (0-100).
     * Rempli pour: EXERCISE_SUBMITTED, QUIZ_ANSWERED.
     * Null pour: CONTENT_READ, AI_QUESTION_ASKED, etc.
     */
    @Column(name = "raw_score")
    private Double rawScore;

    /**
     * Durée de l'interaction en secondes.
     * Rempli pour: CONTENT_READ (temps passé), VIDEO_WATCHED.
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Profondeur de lecture en pourcentage (0-100).
     * Rempli pour: CONTENT_READ.
     */
    @Column(name = "read_depth_percent")
    private Integer readDepthPercent;

    /**
     * Métadonnées supplémentaires en JSON (ex: niveau de la question IA).
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "occurred_at", updatable = false)
    private LocalDateTime occurredAt;
}
