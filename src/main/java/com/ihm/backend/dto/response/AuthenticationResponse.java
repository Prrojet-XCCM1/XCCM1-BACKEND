package com.ihm.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // N'inclut pas les champs null dans le JSON
public class AuthenticationResponse {
    private String id;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String city;
    private String university;
    
    // Champ spécifique aux étudiants (null pour teachers)
    private String specialization;
    
    // Champs spécifiques aux enseignants (null pour students)
    private String grade;
    private List<String> subjects;
    private String certification;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime registrationDate;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime lastLogin;
    
    private String token;
    
    /**
     * Convertit une entité User en AuthenticationResponse
     * Respecte strictement le contrat frontend
     */
    public static AuthenticationResponse fromUser(User user, String token) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        // Conversion des subjects de String vers List<String>
        List<String> subjectsList = null;
        if (user.getSubjects() != null && !user.getSubjects().isEmpty()) {
            // Le format stocké est: "SVT23,Math,Physics" ou JSON array "[\"SVT23\"]"
            if (user.getSubjects().startsWith("[")) {
                // Format JSON array - parser
                subjectsList = parseJsonArray(user.getSubjects());
            } else {
                // Format CSV
                subjectsList = Arrays.asList(user.getSubjects().split(","));
            }
        }
        
        return AuthenticationResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .role(user.getRole().toLowerCase())  // "student" ou "teacher" en minuscules
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .photoUrl(user.getPhotoUrl())
                .city(user.getCity())
                .university(user.getUniversity())
                .specialization(user.getSpecialization())
                .grade(user.getGrade())
                .subjects(subjectsList)
                .certification(user.getCertification())
                .registrationDate(user.getRegistrationDate())
                .lastLogin(user.getLastLogin())
                .token(token)
                .build();
    }
    
    /**
     * Parse un JSON array simple: ["item1","item2"] vers List<String>
     */
    private static List<String> parseJsonArray(String jsonArray) {
        // Simplification: retire les crochets et parse
        String content = jsonArray.substring(1, jsonArray.length() - 1);
        if (content.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(content.split(","))
                .map(s -> s.trim().replaceAll("^\"|\"$", ""))  // Retire les guillemets
                .toList();
    }
}