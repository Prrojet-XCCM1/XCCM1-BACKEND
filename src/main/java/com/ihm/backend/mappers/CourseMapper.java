package com.ihm.backend.mappers;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ihm.backend.dto.request.CourseCreateRequest;
import com.ihm.backend.dto.request.CourseUpdateRequest;
import com.ihm.backend.dto.response.AuthorDTO;
import com.ihm.backend.dto.response.CourseResponse;
import com.ihm.backend.entity.Course;

@Mapper(componentModel = "spring", uses = { AuthorDTO.class })
public interface CourseMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Course toEntity(CourseCreateRequest course);

    @Mapping(target = "author", expression = "java(com.ihm.backend.dto.response.AuthorDTO.fromUser(course.getAuthor()))")
    CourseResponse toResponse(Course course);

    List<CourseResponse> toResponse(List<Course> courses);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(CourseUpdateRequest dto, @MappingTarget Course entity);
}
