package cm.enspy.xccm.domain.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("students")
public class Student extends User {

    @Column("promotion")
    private String promotion;

    @Column("specialization")
    private String specialization;

    @Column("level")
    private String level;

    @Column("average_grade")
    private Double averageGrade;

    @Column("current_semester")
    private Integer currentSemester;

    @Column("major")
    private String major;

    @Column("minor")
    private String minor;

    @Column("interests")
    private String interests; // Stocké comme JSON/String

    @Column("activities")
    private String activities; // Stocké comme JSON/String

    @Column("study_field")
    private String studyField;

    @Column("academic_year")
    private String academicYear;
}