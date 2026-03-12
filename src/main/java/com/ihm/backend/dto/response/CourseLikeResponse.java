package com.ihm.backend.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseLikeResponse {

    private Integer courseId;

    /** true si l'utilisateur connecté a liké ce cours */
    private boolean liked;

    /** Nombre total de likes sur ce cours */
    private long likeCount;
}
