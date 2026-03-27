package com.ihm.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ihm.backend.entity.ClassEnrollment;
import com.ihm.backend.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour l'inscription d'un étudiant à une classe de cours.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassEnrollmentDTO {

    private Long id;

    private Long classId;
    private String className;

    private String studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhotoUrl;

    private EnrollmentStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enrolledAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validatedAt;

    /** Nom de l'enseignant validateur (null si pas encore validé) */
    private String validatedBy;

    /**
     * Convertit une entité ClassEnrollment en DTO
     */
    public static ClassEnrollmentDTO fromEntity(ClassEnrollment entity) {
        if (entity == null) return null;

        return ClassEnrollmentDTO.builder()
                .id(entity.getId())
                .classId(entity.getCourseClass() != null ? entity.getCourseClass().getId() : null)
                .className(entity.getCourseClass() != null ? entity.getCourseClass().getName() : null)
                .studentId(entity.getStudent() != null ? entity.getStudent().getId().toString() : null)
                .studentName(entity.getStudent() != null ? entity.getStudent().getFullName() : null)
                .studentEmail(entity.getStudent() != null ? entity.getStudent().getEmail() : null)
                .studentPhotoUrl(entity.getStudent() != null ? entity.getStudent().getPhotoUrl() : null)
                .status(entity.getStatus())
                .enrolledAt(entity.getEnrolledAt())
                .validatedAt(entity.getValidatedAt())
                .validatedBy(entity.getValidatedBy() != null ? entity.getValidatedBy().getFullName() : null)
                .build();
    }
}
