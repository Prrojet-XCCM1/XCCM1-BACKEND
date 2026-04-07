package com.ihm.backend.enums;

/**
 * Types d'événements comportementaux trackés pour l'évaluation du niveau étudiant.
 *
 * Chaque type a un poids différent dans le calcul du score de connaissance:
 *   - EXERCISE_SUBMITTED : poids 0.6 (signal le plus fort)
 *   - CONTENT_READ       : poids 0.3 (signal intermédiaire)
 *   - AI_QUESTION_ASKED  : poids 0.1 (signal faible mais qualitatif)
 */
public enum BehaviorEventType {

    // === Signaux Actifs (Performance) ===
    /** L'étudiant a soumis un exercice. rawScore requis. */
    EXERCISE_SUBMITTED,

    /** L'étudiant a répondu à une question de quiz automatique. rawScore requis. */
    QUIZ_ANSWERED,

    // === Signaux Passifs (Engagement Contenu) ===
    /** L'étudiant a lu une section de cours. durationSeconds et readDepthPercent requis. */
    CONTENT_READ,

    /** L'étudiant a regardé une vidéo. durationSeconds requis. */
    VIDEO_WATCHED,

    /** L'étudiant a téléchargé un document (signe d'intérêt). */
    DOCUMENT_DOWNLOADED,

    // === Signaux Interactifs (IA Tuteur) ===
    /** L'étudiant a posé une question au tuteur IA. metadata.questionLevel requis. */
    AI_QUESTION_ASKED,

    /** L'étudiant a demandé une reformulation à l'IA (possible incompréhension). */
    AI_REFORMULATION_REQUESTED,

    /** L'étudiant a utilisé la fonctionnalité NotebookLM. */
    NOTEBOOK_ANALYZED
}
