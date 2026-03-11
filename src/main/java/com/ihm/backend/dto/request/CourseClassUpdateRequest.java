package com.ihm.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseClassUpdateRequest {
    private String name;
    private String description;
    private String theme;
    private String coverImage;
    private Integer maxStudents;
}
