package com.ihm.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptanceRequest {
    @NotBlank(message = "Le jeton est obligatoire")
    private String token;
}
