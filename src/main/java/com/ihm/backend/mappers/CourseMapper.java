package com.ihm.backend.mappers;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ihm.backend.DTO.requests.CourseCreateRequestDTO;
import com.ihm.backend.DTO.requests.CourseUpdateRequestDTO;
import com.ihm.backend.DTO.responses.CourseResponse;
import com.ihm.backend.entities.Course;

@Mapper(componentModel = "spring")
public interface CourseMapper{
    @Mapping(target = "id",ignore = true)
    @Mapping(target = "status",constant = "DRAFT")
    @Mapping(target = "createdAt",expression = "java(java.time.LocalDateTime.now())")
    Course toEntity(CourseCreateRequestDTO course);

    CourseResponse toResponse(Course course);
    List<CourseResponse> toResponse(List<Course> courses);
    void updateEntityFromDto(CourseUpdateRequestDTO dto, @MappingTarget Course entity);
}
