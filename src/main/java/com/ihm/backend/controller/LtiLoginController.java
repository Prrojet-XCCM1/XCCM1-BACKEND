package com.ihm.backend.controller;
 
import com.ihm.backend.service.LtiOidcStateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/lti")
@Slf4j
@Tag(name = "LTI", description = "Endpoints LTI 1.3")
@RequiredArgsConstructor
public class LtiLoginController {

    private final LtiOidcStateService oidcStateService;

    @Value("${lti.moodle.platform-url}")
    private String moodleUrl;

    @Value("${lti.moodle.client-id}")
    private String clientId;

    @Value("${lti.xccm1.issuer}")
    private String xccm1Issuer;

    @Operation(summary = "LTI 1.3 OIDC Login Initiation",
               description = "Endpoint appelé par Moodle pour initier le flux d'authentification LTI 1.3")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirection vers l'auth endpoint de Moodle"),
        @ApiResponse(responseCode = "400", description = "Issuer non reconnu ou paramètres manquants")
    })
    @GetMapping("/login")
    public void login(
            @Parameter(description = "URL du LMS émetteur", required = true, example = "http://localhost:3000")
            @RequestParam("iss") String iss,

            @Parameter(description = "Identifiant de l'utilisateur côté LMS", required = true, example = "user123")
            @RequestParam("login_hint") String loginHint,

            @Parameter(description = "URI de lancement après authentification", required = true, example = "http://localhost:8082/lti/launch")
            @RequestParam("target_link_uri") String targetLinkUri,

            @Parameter(description = "Hint de message LTI (optionnel)")
            @RequestParam(value = "lti_message_hint", required = false) String ltiMessageHint,

            HttpServletResponse response) throws IOException {

        log.info("LTI Login Initiation : iss={}, login_hint={}", iss, loginHint);

        if (!issuerMatchesPlatform(iss)) {
            log.warn("Tentative de login LTI avec un issuer inconnu : {} (attendu base {})", iss, moodleUrl);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Issuer non reconnu");
            return;
        }

        String expectedLaunch = trimTrailingSlash(xccm1Issuer) + "/lti/launch";
        String normTarget = targetLinkUri == null ? "" : trimTrailingSlash(targetLinkUri.split("\\?")[0]);
        if (!expectedLaunch.equals(normTarget)) {
            log.warn("target_link_uri invalide : {} (attendu base {})", targetLinkUri, expectedLaunch);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "target_link_uri invalide");
            return;
        }

        String state = oidcStateService.registerState();
        String nonce = UUID.randomUUID().toString();

        String normalizedBase = moodleUrl.endsWith("/") ? moodleUrl.substring(0, moodleUrl.length() - 1) : moodleUrl;
        String authEndpoint = normalizedBase + "/mod/lti/auth.php";

        StringBuilder redirectUrl = new StringBuilder(authEndpoint);
        redirectUrl.append("?response_type=id_token")
                .append("&scope=openid")
                .append("&response_mode=form_post")
                .append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8))
                .append("&redirect_uri=").append(URLEncoder.encode(trimTrailingSlash(xccm1Issuer) + "/lti/launch", StandardCharsets.UTF_8))
                .append("&login_hint=").append(URLEncoder.encode(loginHint, StandardCharsets.UTF_8))
                .append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8))
                .append("&nonce=").append(URLEncoder.encode(nonce, StandardCharsets.UTF_8));

        if (ltiMessageHint != null) {
            redirectUrl.append("&lti_message_hint=").append(URLEncoder.encode(ltiMessageHint, StandardCharsets.UTF_8));
        }

        log.info("Redirection vers Moodle : {}", redirectUrl);
        response.sendRedirect(redirectUrl.toString());
    }

    private boolean issuerMatchesPlatform(String iss) {
        if (iss == null || moodleUrl == null) {
            return false;
        }
        return trimTrailingSlash(iss).equals(trimTrailingSlash(moodleUrl));
    }

    private static String trimTrailingSlash(String url) {
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
