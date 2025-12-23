package com.ihm.backend.dto.request;

import java.util.List;

import com.ihm.backend.enums.UserRole;

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
    
    // Champ spécifique aux étudiants
    private String specialization;
    
    // Champs spécifiques aux enseignants
    private String grade;
    private List<String> subjects;
    private String certification;
}
