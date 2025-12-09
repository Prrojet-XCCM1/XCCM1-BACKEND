package com.ihm.backend.DTO.responses;

import java.time.LocalDateTime;

import com.ihm.backend.entities.User;
import com.ihm.backend.enums.CourseStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class CourseResponse {

      private Integer id;
    private String title;
    private String category;
    private String description;
    
    private CourseStatus status;
   
    private User author;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private String content;
    private String coverImage;
    
}
