package com.ihm.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour l'inscription d'un enseignant
 * Correspond exactement au format frontend pour les enseignants
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRegisterRequest {
    private String email;
    private String password;
    private String confirmPassword;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String city;
    private String university;
    
    // Champs spécifiques aux enseignants
    private String grade;
    private List<String> subjects;
    private String certification;
}
