package cm.enspy.xccm.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtre d'authentification JWT pour l'architecture réactive (WebFlux).
 * Il intercepte chaque requête pour valider et placer le token JWT dans le contexte de sécurité réactif.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveUserDetailsService userDetailsService;

    // Liste des chemins qui ne nécessitent pas d'authentification
    private static final String[] PUBLIC_ROUTES = {
        "/api/v1/auth/", 
        "/swagger-ui/", 
        "/v3/api-docs/", 
        "/actuator/health", 
        "/error",
        "/favicon.ico"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Vérifier si le chemin est public
        for (String route : PUBLIC_ROUTES) {
            if (path.startsWith(route)) {
                return chain.filter(exchange);
            }
        }

        // 2. Extraire le token JWT
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Pas de token JWT trouvé dans la requête: {}", path);
            return chain.filter(exchange);
        }

        // Variables finales pour les lambdas
        final String jwt = authHeader.substring(7);
        final String userEmail = jwtTokenProvider.extractUsername(jwt);

        // 3. Si pas d'email extrait, continuer sans authentification
        if (userEmail == null) {
            log.debug("Impossible d'extraire l'email du token JWT");
            return chain.filter(exchange);
        }

        // 4. Authentification réactive
        return userDetailsService.findByUsername(userEmail)
            .filter(userDetails -> jwtTokenProvider.isTokenValid(jwt, userDetails))
            .map(userDetails -> new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            ))
            .flatMap(authToken -> {
                log.debug("Utilisateur authentifié: {} pour URI: {}", userEmail, path);
                
                // Continuer avec le contexte de sécurité mis à jour
                return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
            })
            // Si l'authentification échoue, continuer sans contexte
            .switchIfEmpty(Mono.defer(() -> {
                log.debug("Authentification échouée pour: {}", userEmail);
                return chain.filter(exchange);
            }));
    }
}
