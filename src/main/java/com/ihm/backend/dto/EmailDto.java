package com.ihm.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import com.ihm.backend.enums.EmailTemplate;

/**
 * DTO pour l'envoi d'emails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDto {
    private String to;
    private String subject;
    private String body;
    private EmailTemplate template;
    private Map<String, Object> templateVariables;
    private boolean isHtml;
}