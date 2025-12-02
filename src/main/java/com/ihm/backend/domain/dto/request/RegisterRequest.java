package cm.enspy.xccm.domain.dto.request;

import java.util.List;

import cm.enspy.xccm.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String email;
    private String password;
    private String confirmPassword;
    private UserRole role;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String city;
    private String university;
    
    // Champs communs optionnels
    private String phoneNumber;
    private String bio;
    
 
    
    // Champs spécifiques aux enseignants
    private String grade;
    private String certification;
    private List<String> subjects;
    private List<String> teachingGrades;
    private String teachingGoal;
    private String department;
    private Integer yearsOfExperience;
    private String officeLocation;
    private String officeHours;




    // Student fields
    private String promotion;
    private String specialization;
    private String level;
    private String averageGrade;
    private String currentSemester;
    private String major;
    private String minor;
    private String studyField;
    private String academicYear;
    private List<String> interests;
    private List<String> activities;
    
   

}
