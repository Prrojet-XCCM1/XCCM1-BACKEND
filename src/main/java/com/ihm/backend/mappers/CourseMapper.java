package com.ihm.backend.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ihm.backend.dto.request.CourseCreateRequest;
import com.ihm.backend.dto.request.CourseUpdateRequest;
import com.ihm.backend.dto.response.AuthorDTO;
import com.ihm.backend.dto.response.CourseResponse;
import com.ihm.backend.entity.Course;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "courseClass", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Course toEntity(CourseCreateRequest course);

    @Mapping(target = "author", expression = "java(com.ihm.backend.dto.response.AuthorDTO.fromUser(course.getAuthor()))")
    @Mapping(target = "classId", expression = "java(course.getCourseClass() != null ? course.getCourseClass().getId() : null)")
    @Mapping(target = "className", expression = "java(course.getCourseClass() != null ? course.getCourseClass().getName() : null)")
    CourseResponse toResponse(Course course);

    List<CourseResponse> toResponse(List<Course> courses);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "courseClass", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "content", ignore = true) // Ignorer pour le forcer manuellement et déclencher la sauvegarde
    void updateEntity(CourseUpdateRequest dto, @MappingTarget Course entity);

    @org.mapstruct.AfterMapping
    default void updateContent(CourseUpdateRequest dto, @MappingTarget Course entity) {
        if (dto.getContent() != null) {
            // Instancier une NOUVELLE Map pour forcer le Dirty Checking d'Hibernate
            // Sinon, map.clear() puis map.putAll() est ignoré par PostgreSQL
            entity.setContent(new java.util.LinkedHashMap<>(dto.getContent()));
        } else {
            entity.setContent(null);
        }
    }
}
