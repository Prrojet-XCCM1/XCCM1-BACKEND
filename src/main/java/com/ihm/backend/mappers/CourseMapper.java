package com.ihm.backend.mappers;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ihm.backend.dto.request.CourseCreateRequest;
import com.ihm.backend.dto.request.CourseUpdateRequest;
import com.ihm.backend.dto.response.CourseResponse;
import com.ihm.backend.entity.Course;

@Mapper(componentModel = "spring")
public interface CourseMapper{
    @Mapping(target = "id",ignore = true)
    @Mapping(target = "status",constant = "DRAFT")
    @Mapping(target = "createdAt",expression = "java(java.time.LocalDateTime.now())")
    Course toEntity(CourseCreateRequest course);

    CourseResponse toResponse(Course course);
    List<CourseResponse> toResponse(List<Course> courses);
    void updateEntity(CourseUpdateRequest dto, @MappingTarget Course entity);
}
