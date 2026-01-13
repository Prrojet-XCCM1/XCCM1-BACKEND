package com.ihm.backend.dto.response;

import com.ihm.backend.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {

    private UUID id;
    private String email;
    private UserRole role;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String city;
    private String university;

    // Champs spécifiques aux étudiants
    private String specialization;

    // Champs spécifiques aux enseignants
    private String grade;
    private List<String> subjects;
    private String certification;

    private LocalDateTime registrationDate;
    private LocalDateTime lastLogin;
    private boolean active;
    private boolean verified;
}
