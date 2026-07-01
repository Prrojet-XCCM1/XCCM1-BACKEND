package com.ihm.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseAIGenerateRequest {

    private String description;
    private String discipline = "general";
    private String level = "L1";
    private String language = "fr";
    private int exercisesPerChapter = 1;

    /**
     * ID du cours à mettre à jour après la génération (auto-save).
     * Si null, le contenu n'est pas automatiquement persisté.
     */
    private Integer courseId;
}
