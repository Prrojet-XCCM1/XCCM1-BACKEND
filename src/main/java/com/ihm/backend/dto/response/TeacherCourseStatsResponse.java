package com.ihm.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCourseStatsResponse {
    
    private Integer courseId;
    private String courseTitle;
    private String courseCategory;
    
    // Enrollment statistics
    private long totalEnrolled;
    private long activeStudents;  // Students with recent activity
    private Double participationRate;  // Percentage of active students
    
    // Progress statistics
    private Double averageProgress;
    private long completedStudents;
    
    // Exercise statistics
    private long totalExercises;
    private List<ExerciseStatsDTO> exerciseStats;
    
    // Performance distribution
    private PerformanceDistributionDTO performanceDistribution;
}
