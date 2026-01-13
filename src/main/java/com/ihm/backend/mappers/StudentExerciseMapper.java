package com.ihm.backend.mappers;

import com.ihm.backend.dto.response.StudentExerciseResponse;
import com.ihm.backend.entity.StudentExercise;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentExerciseMapper {

    @Mapping(target = "exerciseId", source = "exercise.id")
    @Mapping(target = "exerciseTitle", source = "exercise.title")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", expression = "java(studentExercise.getStudent().getFirstName() + \" \" + studentExercise.getStudent().getLastName())")
    @Mapping(target = "maxScore", source = "exercise.maxScore")
    StudentExerciseResponse toResponse(StudentExercise studentExercise);
}
