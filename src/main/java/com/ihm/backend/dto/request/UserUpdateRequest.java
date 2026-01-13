package com.ihm.backend.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    private String firstName;
    private String lastName;

    @Email(message = "Email invalide")
    private String email;

    private String city;
    private String university;
    private String photoUrl;

    // Champs spécifiques aux étudiants
    private String specialization;

    // Champs spécifiques aux enseignants
    private String grade;
    private List<String> subjects;
    private String certification;

    // Champ pour activer/désactiver un utilisateur
    private Boolean active;
}
