package com.ihm.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ihm.backend.dto.ClassEnrollmentDTO;
import com.ihm.backend.entity.CourseClass;
import com.ihm.backend.enums.ClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Réponse enrichie pour une classe de cours.
 * Contient les infos de la classe, ses cours, les compteurs d'inscrits,
 * et optionnellement l'inscription de l'étudiant courant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseClassResponse {

    private Long id;
    private String name;
    private String description;
    private String theme;
    private String coverImage;
    private ClassStatus status;

    /** Enseignant propriétaire */
    private AuthorDTO teacher;

    /** Liste des cours de la classe */
    private List<CourseResponse> courses;

    /** Nombre de participants (inscrits distincts dans les cours de la classe) */
    private Long participantCount;

    /** Nombre de demandes en attente (PENDING) */
    private Long pendingCount;

    /** Nombre maximum d'étudiants (0 = illimité, non utilisé comme limite stricte) */
    private Integer maxStudents;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Inscription de l'étudiant courant à cette classe (null si non inscrit ou non étudiant)
     */
    private ClassEnrollmentDTO myEnrollment;

    /**
     * Construit une réponse simple (sans enrollment et sans compteurs dynamiques)
     */
    public static CourseClassResponse fromEntity(CourseClass entity, Long studentCount, Long pendingCount) {
        return CourseClassResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .theme(entity.getTheme())
                .coverImage(entity.getCoverImage())
                .status(entity.getStatus())
                .teacher(AuthorDTO.fromUser(entity.getTeacher()))
                .courses(entity.getCourses() != null
                        ? entity.getCourses().stream()
                                .map(c -> {
                                    CourseResponse r = new CourseResponse();
                                    r.setId(c.getId());
                                    r.setTitle(c.getTitle());
                                    r.setCategory(c.getCategory());
                                    r.setDescription(c.getDescription());
                                    r.setStatus(c.getStatus());
                                    r.setAuthor(AuthorDTO.fromUser(c.getAuthor()));
                                    r.setCoverImage(c.getCoverImage());
                                    r.setPhotoUrl(c.getPhotoUrl());
                                    r.setViewCount(c.getViewCount());
                                    r.setLikeCount(c.getLikeCount());
                                    r.setDownloadCount(c.getDownloadCount());
                                    r.setCreatedAt(c.getCreatedAt());
                                    r.setPublishedAt(c.getPublishedAt());
                                    r.setContent(c.getContent());
                                    return r;
                                })
                                .collect(Collectors.toList())
                        : List.of())
                .participantCount(studentCount)
                .pendingCount(pendingCount)
                .maxStudents(entity.getMaxStudents())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
