package com.ihm.backend.dto.response;

import com.ihm.backend.enums.CourseGenerationJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseGenerationJobCreatedResponse {
    private UUID jobId;
    private CourseGenerationJobStatus status;
}
