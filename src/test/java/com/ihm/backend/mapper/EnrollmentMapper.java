package com.ihm.backend.mapper;

import com.ihm.backend.dto.EnrollmentDTO;
import com.ihm.backend.entity.Enrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "etudiantId", source = "etudiant.id")
    @Mapping(target = "coursId", source = "cours.id")
    EnrollmentDTO toDTO(Enrollment enrollment);

    @Mapping(target = "etudiant", ignore = true)  // Ignorer, à set manuellement dans service
    @Mapping(target = "cours", ignore = true)
    Enrollment toEntity(EnrollmentDTO dto);
}