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
@Table("teachers")
public class Teacher extends User {

    @Column("grade")
    private String grade;

    @Column("certification")
    private String certification;

    @Column("teaching_goal")
    private String teachingGoal;

    @Column("subjects")
    private String subjects; // Stocké comme JSON/String

    @Column("teaching_grades")
    private String teachingGrades; // Stocké comme JSON/String

    @Column("department")
    private String department;

    @Column("years_of_experience")
    private Integer yearsOfExperience;

    @Column("office_location")
    private String officeLocation;

    @Column("office_hours")
    private String officeHours;
}