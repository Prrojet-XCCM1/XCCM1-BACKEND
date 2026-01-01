package com.ihm.backend.dto.request;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseUpdateRequest {
    private String title;
    private String category;
    private String description;
    private String content;
    
}
