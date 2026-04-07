package com.ihm.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseStatsDTO {
    
    private Integer exerciseId;
    private String title;
    private long submissionCount;
    private Double averageScore;
    private Double minScore;
    private Double maxScore;
    private Double maxPossibleScore;
}
