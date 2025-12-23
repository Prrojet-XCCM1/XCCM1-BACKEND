package com.ihm.backend.mappers;

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