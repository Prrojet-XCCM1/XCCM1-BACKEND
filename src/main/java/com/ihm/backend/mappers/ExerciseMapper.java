package com.ihm.backend.mappers;

import com.ihm.backend.dto.request.ExerciseCreateRequest;
import com.ihm.backend.dto.request.ExerciseUpdateRequest;
import com.ihm.backend.dto.response.ExerciseResponse;
import com.ihm.backend.entity.Exercise;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ExerciseMapper {

    @Mapping(target = "course", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Exercise toEntity(ExerciseCreateRequest request);

    @Mapping(target = "courseId", source = "course.id")
    ExerciseResponse toResponse(Exercise exercise);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(ExerciseUpdateRequest request, @MappingTarget Exercise exercise);

    default String mapMapToString(java.util.Map<String, Object> content) {
        return com.ihm.backend.utils.JsonUtils.toJson(content);
    }

    default java.util.Map<String, Object> mapStringToMap(String content) {
        return com.ihm.backend.utils.JsonUtils.toMap(content);
    }
}
