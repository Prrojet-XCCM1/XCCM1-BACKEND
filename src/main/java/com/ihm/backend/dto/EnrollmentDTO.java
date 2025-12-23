package com.ihm.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ihm.backend.entity.Enrollment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO correspondant exactement au contrat frontend TypeScript:
 * interface Enrollment {
 *   courseId: number;
 *   userId: string;
 *   enrolledAt: string;
 *   progress: number;
 *   lastAccessed?: string;
 *   completed?: boolean;
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrollmentDTO {
    
    private Integer courseId;
    
    private String userId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime enrolledAt;
    
    private Double progress;  // 0-100
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime lastAccessed;
    
    private Boolean completed;
    
    /**
     * Convertit une entité Enrollment en DTO
     */
    public static EnrollmentDTO fromEntity(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }
        
        return EnrollmentDTO.builder()
                .courseId(enrollment.getCourseId())
                .userId(enrollment.getUserId() != null ? enrollment.getUserId().toString() : null)
                .enrolledAt(enrollment.getEnrolledAt())
                .progress(enrollment.getProgress())
                .lastAccessed(enrollment.getLastAccessed())
                .completed(enrollment.getCompleted())
                .build();
    }
}