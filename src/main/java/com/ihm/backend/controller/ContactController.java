package com.ihm.backend.controller;

import com.ihm.backend.dto.request.ContactRequest;
import com.ihm.backend.dto.request.NewsletterRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public Services", description = "API pour les formulaires de contact et newsletter")
public class ContactController {

    private final NotificationService notificationService;

    @Operation(summary = "Formulaire de contact")
    @PostMapping("/contact")
    public ResponseEntity<ApiResponse<Void>> contactUs(@RequestBody @Valid ContactRequest request) {
        log.info("Demande de contact reçue de: {}", request.getEmail());
        notificationService.sendContactAcknowledgement(request.getName(), request.getEmail(), request.getSubject());

        // On pourrait aussi notifier l'admin ici si besoin
        // notificationService.notifyAdmin(request);

        return ResponseEntity.ok(ApiResponse.success("Votre message a été envoyé avec succès"));
    }

    @Operation(summary = "Inscription à la newsletter")
    @PostMapping("/newsletter")
    public ResponseEntity<ApiResponse<Void>> subscribeNewsletter(@RequestBody @Valid NewsletterRequest request) {
        log.info("Nouvelle inscription newsletter: {}", request.getEmail());
        notificationService.sendNewsletterConfirmation(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Succès ! Vous êtes maintenant inscrit à notre newsletter"));
    }
}
