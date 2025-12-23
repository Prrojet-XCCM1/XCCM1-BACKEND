package com.ihm.backend.dto.response;

import com.ihm.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour représenter l'auteur d'un cours dans EnrichedCourse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDTO {
    
    private String name;        // fullName de l'utilisateur
    
    private String image;       // photoUrl
    
    private String designation; // grade pour les enseignants
    
    /**
     * Convertit un User (teacher) en AuthorDTO
     */
    public static AuthorDTO fromUser(User user) {
        if (user == null) {
            return null;
        }
        
        return AuthorDTO.builder()
                .name(user.getFullName())
                .image(user.getPhotoUrl())
                .designation(user.getGrade())  // Sera null pour les non-teachers
                .build();
    }
}
