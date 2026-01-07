package com.ihm.backend.dto.response;

import java.time.LocalDateTime;

import com.ihm.backend.entity.User;
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

  private AuthorDTO author;
  private LocalDateTime createdAt;
  private LocalDateTime publishedAt;
  private java.util.Map<String, Object> content;
  private String coverImage;

}
