package com.ihm.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseCreateRequest {

    private String title;
    private String category;
    private String description;
    private java.util.Map<String, Object> content;
    private String photoUrl;
}
