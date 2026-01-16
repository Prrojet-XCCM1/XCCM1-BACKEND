package com.ihm.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseResponse {
    private Integer id;
    private Integer courseId;
    private String title;
    private String description;
    private Double maxScore;
    private LocalDateTime dueDate;
    private String content;
    private LocalDateTime createdAt;
}
