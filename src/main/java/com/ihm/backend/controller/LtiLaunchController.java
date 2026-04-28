package com.ihm.backend.controller;
 
import com.ihm.backend.service.LtiAccountMappingService;
import com.ihm.backend.service.LtiJwtValidatorService;
import com.ihm.backend.service.LtiOidcStateService;

import com.nimbusds.jwt.JWTClaimsSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/lti")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "LTI", description = "Endpoints LTI 1.3")
public class LtiLaunchController {

    private final LtiJwtValidatorService ltiJwtValidatorService;
    private final LtiAccountMappingService ltiAccountMappingService;
    private final LtiOidcStateService ltiOidcStateService;

    @Operation(summary = "LTI 1.3 Launch", description = "Reçoit l'id_token envoyé par Moodle, valide le JWT RSA256, et génère un token XCCM1 (SSO)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification réussie, token XCCM1 retourné"),
            @ApiResponse(responseCode = "400", description = "id_token manquant ou state OIDC invalide"),
            @ApiResponse(responseCode = "401", description = "JWT invalide ou authentification LTI échouée")
    })
    @PostMapping(value = "/launch", consumes = { "application/x-www-form-urlencoded", "application/json", "*/*" })
    public ResponseEntity<?> launch(
            @Parameter(description = "JWT signé envoyé par Moodle (id_token LTI 1.3)", required = true) @RequestParam(value = "id_token", required = false) String idToken,

            @Parameter(description = "Valeur de corrélation OIDC générée lors du login initiation", required = false) @RequestParam(value = "state", required = false) String state) {

        if (idToken == null || idToken.isBlank()) {
            log.error("Lancement LTI échoué : id_token manquant");
            return ResponseEntity.badRequest().body(Map.of("error", "id_token manquant"));
        }

        log.info("Lancement LTI reçu (state={}). Validation et mapping en cours...", state);

        try {
            ltiOidcStateService.validateAndConsume(state);
        } catch (IllegalArgumentException e) {
            log.warn("State OIDC invalide : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }

        try {
            // 1. Validation mathématique et sémantique du JWT (RSA256) via JWKS Moodle
            JWTClaimsSet claims = ltiJwtValidatorService.validateLtiJwt(idToken);

            // 2. Mapping de compte & Génération du token XCCM1 (SSO)
            String xccm1Token = ltiAccountMappingService.resolveAndGenerateToken(claims);

            log.info("SSO réussi pour l'utilisateur Moodle {}. Token XCCM1 généré.", claims.getSubject());

            // 3. Réponse finale vers le frontend (ou redirection directe)
            // Pour l'intégration iframe, on retourne le token que le frontend Next.js
            // récupérera.
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "token", xccm1Token,
                    "user", Map.of(
                            "email", claims.getClaim("email") != null ? claims.getClaim("email") : claims.getSubject(),
                            "name", claims.getClaim("name") != null ? claims.getClaim("name") : "Utilisateur Moodle")));

        } catch (Exception e) {
            log.error("Échec du lancement LTI : {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Authentification LTI échouée : " + e.getMessage()));
        }
    }
}
