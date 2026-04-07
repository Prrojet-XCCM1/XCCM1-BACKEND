package com.ihm.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseCommentRequest {

    @NotBlank(message = "Le contenu du commentaire ne peut pas être vide")
    @Size(min = 1, max = 2000, message = "Le commentaire doit contenir entre 1 et 2000 caractères")
    private String content;
}
