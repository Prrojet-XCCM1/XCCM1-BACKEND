package com.ihm.backend.mappers;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ihm.backend.dto.UserDto;
import com.ihm.backend.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // Conversion UUID <-> String
    default String map(UUID uuid) {
        return uuid != null ? uuid.toString() : null; 
    }
    
    default UUID map(String string) {
        return string != null ? UUID.fromString(string) : null;
    }
    
    // Conversion String <-> List<String> pour le champ subjects
    default List<String> stringToList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        // Support pour format CSV: "Math,Physics,SVT"
        return Arrays.asList(value.split(","));
    }
    
    default String listToString(List<String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        // Convertit List<String> vers CSV: "Math,Physics,SVT"
        return String.join(",", value);
    }

    UserDto toDto(User user);
    
    User toEntity(UserDto dto); 

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "firstName")
    @Mapping(target = "lastName")
    @Mapping(target = "email")
    @Mapping(target = "role")
    @Mapping(target = "photoUrl")
    @Mapping(source = "isActive", target = "active") 
    void updateUserFromDto(UserDto dto, @MappingTarget User user);
}