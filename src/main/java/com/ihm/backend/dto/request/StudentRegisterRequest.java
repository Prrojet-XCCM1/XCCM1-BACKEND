package com.ihm.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'inscription d'un étudiant
 * Correspond exactement au format frontend pour les étudiants
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegisterRequest {
    private String email;
    private String password;
    private String confirmPassword;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String city;
    private String university;
    
    // Champ spécifique aux étudiants
    private String specialization;
}
