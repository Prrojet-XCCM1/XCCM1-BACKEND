package com.ihm.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ihm.backend.enums.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private String id;
    private String email;
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
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime registrationDate;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime lastLogin;
    
    private Boolean isActive;
}
