package com.ihm.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatsResponse {

    private long totalEnrollments;
    private Map<String, Long> byStatus;
    private List<CourseEnrollmentStat> byCourse;
    private Double averageProgress;
    private Double completionRate;
    private long recentEnrollments; // 7 derniers jours
    private long pendingEnrollments;
    private long acceptedEnrollments;
    private long rejectedEnrollments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseEnrollmentStat {
        private Integer courseId;
        private String courseTitle;
        private long enrollmentCount;
        private Double averageProgress;
    }
}
