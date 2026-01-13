package com.ihm.backend.dto.request;

import com.ihm.backend.enums.CourseStatus;
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
    private CourseStatus status;
    private java.util.Map<String, Object> content;
    private String coverImage;
}
