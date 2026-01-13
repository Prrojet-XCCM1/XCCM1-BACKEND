package com.ihm.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExerciseResponse {
    private Long id;
    private Integer exerciseId;
    private String exerciseTitle;
    private UUID studentId;
    private String studentName;
    private Double score;
    private Double maxScore;
    private String feedback;
    private String submissionUrl;
    private LocalDateTime submittedAt;
}
