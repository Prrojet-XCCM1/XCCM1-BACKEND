package com.ihm.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ihm.backend.entity.CourseComment;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseCommentDTO {

    private Long id;
    private Integer courseId;
    private String userId;
    private String userFullName;
    private String userPhotoUrl;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;

    public static CourseCommentDTO fromEntity(CourseComment comment) {
        if (comment == null) return null;
        return CourseCommentDTO.builder()
                .id(comment.getId())
                .courseId(comment.getCourse() != null ? comment.getCourse().getId() : null)
                .userId(comment.getUser() != null ? comment.getUser().getId().toString() : null)
                .userFullName(comment.getUser() != null ? comment.getUser().getFullName() : null)
                .userPhotoUrl(comment.getUser() != null ? comment.getUser().getPhotoUrl() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
