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
    @jakarta.validation.constraints.NotBlank(message = "L'email est obligatoire")
    @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "L'email doit respecter le format standard (ex: user@domain.com)")
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
