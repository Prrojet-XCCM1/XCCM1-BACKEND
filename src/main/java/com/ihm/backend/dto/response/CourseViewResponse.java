package com.ihm.backend.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseViewResponse {

    private Integer courseId;

    /** true si la vue a bien été enregistrée (premier appel), false si elle existait déjà */
    private boolean recorded;

    /** Nombre total de vues uniques sur ce cours */
    private long viewCount;
}
