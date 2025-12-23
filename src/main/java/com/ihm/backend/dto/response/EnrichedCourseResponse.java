package com.ihm.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ihm.backend.dto.EnrollmentDTO;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.Enrollment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO correspondant au contrat frontend TypeScript:
 * interface EnrichedCourse {
 *   id: number;
 *   title: string;
 *   category: string;
 *   image: string;
 *   author: {
 *     name: string;
 *     image: string;
 *     designation?: string;
 *   };
 *   enrollment?: Enrollment;
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrichedCourseResponse {
    
    private Integer id;
    
    private String title;
    
    private String category;
    
    private String image;  // Mappé depuis coverImage
    
    private AuthorDTO author;
    
    private EnrollmentDTO enrollment;  // Null si l'utilisateur n'est pas enrôlé
    
    /**
     * Crée un EnrichedCourseResponse à partir d'un cours et d'un enrôlement optionnel
     */
    public static EnrichedCourseResponse fromCourse(Course course, Enrollment enrollment) {
        if (course == null) {
            return null;
        }
        
        return EnrichedCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .category(course.getCategory())
                .image(course.getCoverImage())
                .author(AuthorDTO.fromUser(course.getAuthor()))
                .enrollment(EnrollmentDTO.fromEntity(enrollment))
                .build();
    }
}
