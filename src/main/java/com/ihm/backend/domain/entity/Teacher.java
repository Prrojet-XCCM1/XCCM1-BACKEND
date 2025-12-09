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
@DiscriminatorValue("TEACHER")
public class Teacher extends User {

    private String grade;

    private String certification;

    @Column(name = "teaching_goal")
    private String teachingGoal;

    private String subjects;         // JSON string

    @Column(name = "teaching_grades")
    private String teachingGrades;   // JSON string

    private String department;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "office_location")
    private String officeLocation;

    @Column(name = "office_hours")
    private String officeHours;
}