package com.ihm.backend.domain.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.ihm.backend.domain.entity.Student;
import com.ihm.backend.domain.entity.Teacher;
import com.ihm.backend.domain.enums.UserRole;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private String token;
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private String photoUrl;
    private String type;
    private Object userDetails;
    
    public static AuthenticationResponse fromUser(Object user, String token) {
        if (user instanceof Student student) {
            return AuthenticationResponse.builder()
                    .token(token)
                    .id(student.getId())
                    .email(student.getEmail())
                    .firstName(student.getFirstName())
                    .lastName(student.getLastName())
                    .role(student.getRole())
                    .type("STUDENT")
                    .userDetails(student)
                    .build();
        } else if (user instanceof Teacher teacher) {
            return AuthenticationResponse.builder()
                    .token(token)
                    .id(teacher.getId())
                    .email(teacher.getEmail())
                    .firstName(teacher.getFirstName())
                    .lastName(teacher.getLastName())
                    .role(teacher.getRole())
                    .photoUrl(teacher.getPhotoUrl())
                    .type("TEACHER")
                    .userDetails(teacher)
                    .build();
        }
        throw new IllegalArgumentException("Type d'utilisateur non supporté");
    }
}