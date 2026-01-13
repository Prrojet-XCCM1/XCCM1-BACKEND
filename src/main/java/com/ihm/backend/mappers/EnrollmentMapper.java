package com.ihm.backend.mappers;

import com.ihm.backend.dto.response.EnrollmentDetailResponse;
import com.ihm.backend.entity.Enrollment;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMapper {

    public EnrollmentDetailResponse toDetailResponse(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }

        return EnrollmentDetailResponse.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUser() != null ? enrollment.getUser().getId() : null)
                .userFullName(enrollment.getUser() != null ? enrollment.getUser().getFullName() : null)
                .userEmail(enrollment.getUser() != null ? enrollment.getUser().getEmail() : null)
                .courseId(enrollment.getCourse() != null ? enrollment.getCourse().getId() : null)
                .courseTitle(enrollment.getCourse() != null ? enrollment.getCourse().getTitle() : null)
                .courseCategory(enrollment.getCourse() != null ? enrollment.getCourse().getCategory() : null)
                .enrolledAt(enrollment.getEnrolledAt())
                .progress(enrollment.getProgress())
                .lastAccessed(enrollment.getLastAccessed())
                .completed(enrollment.getCompleted())
                .status(enrollment.getStatus())
                .build();
    }
}
