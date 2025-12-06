package com.ihm.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EnrollmentDTO {
    private Long id;
    private Long etudiantId;
    private Long coursId;
    private LocalDateTime enrollmentDate;
    private String status;
    private double progress;
}