package com.ihm.backend.controller;
 
import com.ihm.backend.service.LtiKeyService;

import com.nimbusds.jose.jwk.JWKSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/lti/.well-known")
@RequiredArgsConstructor
@Tag(name = "LTI", description = "Endpoints LTI 1.3")
public class LtiWellKnownController {

    private final LtiKeyService ltiKeyService;

    @Operation(summary = "LTI JWKS (clé publique RSA)", description = "Expose la clé publique de XCCM1 au format JWKS. Moodle l'utilise pour vérifier les messages signés.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "JWK Set retourné avec succès"))
    @GetMapping("/jwks.json")
    public Map<String, Object> getJwks() {
        JWKSet jwkSet = ltiKeyService.getPublicJwkSet();
        return jwkSet.toJSONObject();
    }
}
