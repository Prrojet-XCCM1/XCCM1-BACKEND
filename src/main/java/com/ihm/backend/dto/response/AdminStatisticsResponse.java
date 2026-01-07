package com.ihm.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatisticsResponse {

    // User stats
    private long totalUsers;
    private long studentCount;
    private long teacherCount;

    // Course stats
    private long totalCourses;
    private long publishedCourses;
    private long draftCourses;

    // Enrollment stats
    private long totalEnrollments;
    private long pendingEnrollments;
    private long approvedEnrollments;
    private long rejectedEnrollments;
}
