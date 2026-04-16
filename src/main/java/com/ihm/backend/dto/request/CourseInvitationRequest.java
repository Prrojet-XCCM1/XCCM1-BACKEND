package com.ihm.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseInvitationRequest {
    @NotNull(message = "Le cours est obligatoire")
    private Integer courseId;
    
    @NotBlank(message = "L'email ou le nom est obligatoire")
    private String emailOrName;
}
