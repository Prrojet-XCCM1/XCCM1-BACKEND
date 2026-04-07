package com.ihm.backend.dto;

import com.ihm.backend.enums.BehaviorEventType;
import lombok.Data;

/**
 * DTO reçu par le Frontend pour enregistrer un événement comportemental.
 */
@Data
public class BehaviorEventRequest {

    /** Type d'événement (ex: CONTENT_READ, EXERCISE_SUBMITTED). */
    private BehaviorEventType eventType;

    /** Identifiant du granule concerné (provient du LLM Service). */
    private Integer granuleId;

    /**
     * Notion/concept concerné.
     * Doit correspondre à une notion connue dans StudentKnowledgeState.
     * Ex: "Algorithmes de tri", "Réseaux de neurones".
     */
    private String notion;

    /**
     * Score brut (0-100). Obligatoire pour EXERCISE_SUBMITTED et QUIZ_ANSWERED.
     */
    private Double rawScore;

    /** Temps passé en secondes. Obligatoire pour CONTENT_READ et VIDEO_WATCHED. */
    private Integer durationSeconds;

    /** Profondeur de lecture en % (0-100). Obligatoire pour CONTENT_READ. */
    private Integer readDepthPercent;

    /** Données JSON supplémentaires (ex: niveau de question IA: BEGINNER/INTERMEDIATE/ADVANCED). */
    private String metadata;
}
