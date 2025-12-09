package com.ihm.backend.entities;

import java.time.LocalDateTime;

import com.ihm.backend.enums.CourseStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private String category;
    private String description;
    @Enumerated(value = EnumType.STRING)
    private CourseStatus status;
    @ManyToOne
    private User author;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private String content;
    private String coverImage;

}
