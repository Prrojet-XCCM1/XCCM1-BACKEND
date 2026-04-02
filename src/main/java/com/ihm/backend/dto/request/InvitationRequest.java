package com.ihm.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRequest {
    @NotNull(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    @NotNull(message = "L'ID du cours/document est obligatoire")
    private Integer courseId;
}
