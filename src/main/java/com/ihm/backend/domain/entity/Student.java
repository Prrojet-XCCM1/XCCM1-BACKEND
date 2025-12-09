package com.ihm.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@DiscriminatorValue("STUDENT")
public class Student extends User {

    private String promotion;

    private String specialization;

    private String level;

    @Column(name = "average_grade")
    private Double averageGrade;

    @Column(name = "current_semester")
    private Integer currentSemester;

    private String major;

    private String minor;

    private String interests;        // JSON string

    private String activities;       // JSON string

    @Column(name = "study_field")
    private String studyField;

    @Column(name = "academic_year")
    private String academicYear;
}