package com.ihm.backend.dto.response;

import com.ihm.backend.enums.CourseGenerationJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseGenerationJobResponse {
    private UUID jobId;
    private CourseGenerationJobStatus status;
    private String progressEvent;
    private String progressMessage;
    private Integer progressPercent;
    private Map<String, Object> result;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
