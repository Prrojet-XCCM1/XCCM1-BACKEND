package cm.enspy.xccm.controller;

import org.springframework.web.bind.annotation.*;
import cm.enspy.xccm.domain.dto.request.*;
import cm.enspy.xccm.domain.dto.response.AuthenticationResponse;
import cm.enspy.xccm.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public Mono<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }

    @PostMapping("/authenticate")
    public Mono<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return authenticationService.authenticate(request);
    }

    @PostMapping("/password-reset-request")
    public Mono<String> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        return authenticationService.requestPasswordReset(request);
    }

    @PostMapping("/reset-password")
    public Mono<String> resetPassword(@RequestBody PasswordUpdateRequest request) {
        return authenticationService.resetPassword(request);
    }
}