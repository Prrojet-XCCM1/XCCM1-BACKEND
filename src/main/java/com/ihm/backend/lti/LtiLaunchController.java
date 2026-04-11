package com.ihm.backend.lti;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/lti")
@RequiredArgsConstructor
@Slf4j
public class LtiLaunchController {

    private final LtiJwtValidatorService ltiJwtValidatorService;
    private final LtiAccountMappingService ltiAccountMappingService;
    private final LtiOidcStateService ltiOidcStateService;

    /**
     * Endpoint de réception du lancement LTI 1.3 (id_token).
     * Moodle envoie un POST avec id_token (et state).
     */
    @PostMapping("/launch")
    public ResponseEntity<?> launch(HttpServletRequest request) {
        String idToken = request.getParameter("id_token");
        String state = request.getParameter("state");

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
            // Pour l'intégration iframe, on retourne le token que le frontend Next.js récupérera.
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "token", xccm1Token,
                "user", Map.of(
                    "email", claims.getClaim("email") != null ? claims.getClaim("email") : claims.getSubject(),
                    "name", claims.getClaim("name") != null ? claims.getClaim("name") : "Utilisateur Moodle"
                )
            ));

        } catch (Exception e) {
            log.error("Échec du lancement LTI : {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Authentification LTI échouée : " + e.getMessage()));
        }
    }
}
