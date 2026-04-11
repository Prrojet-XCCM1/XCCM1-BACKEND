package com.ihm.backend.lti;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/lti/.well-known")
@RequiredArgsConstructor
public class LtiWellKnownController {

    private final LtiKeyService ltiKeyService;

    /**
     * Endpoint public qui expose la clé publique de XCCM1 au format JWKS.
     * Moodle utilise cet endpoint pour authentifier les messages signés par XCCM1.
     */
    @GetMapping("/jwks.json")
    public Map<String, Object> getJwks() {
        JWKSet jwkSet = ltiKeyService.getPublicJwkSet();
        return jwkSet.toJSONObject();
    }
}
