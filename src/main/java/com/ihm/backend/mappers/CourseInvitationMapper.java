package com.ihm.backend.mappers;

import com.ihm.backend.dto.response.CourseInvitationResponse;
import com.ihm.backend.entity.CourseInvitation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseInvitationMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "inviterName", expression = "java(invitation.getInviter().getFullName())")
    CourseInvitationResponse toResponse(CourseInvitation invitation);
}
