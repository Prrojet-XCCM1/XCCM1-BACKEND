package com.ihm.backend.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

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
  private Map<String, Object> content;
  private String coverImage;
  private String photoUrl;
  private Long viewCount;
  private Long likeCount;
  private Long downloadCount;

  /** ID de la classe à laquelle ce cours appartient (null si standalone) */
  private Long classId;

  /** Nom de la classe (null si le cours n'est pas rattaché à une classe) */
  private String className;

}
