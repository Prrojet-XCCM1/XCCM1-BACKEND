package com.ihm.backend.dto.response;

import com.ihm.backend.enums.EnrollmentStatus;
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
public class EnrollmentDetailResponse {

    private Long id;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private Integer courseId;
    private String courseTitle;
    private String courseCategory;
    private LocalDateTime enrolledAt;
    private Double progress;
    private LocalDateTime lastAccessed;
    private Boolean completed;
    private EnrollmentStatus status;
}
