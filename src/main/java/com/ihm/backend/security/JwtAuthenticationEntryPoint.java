package cm.enspy.xccm.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> commence(
            ServerWebExchange exchange,
            AuthenticationException authException
    ) {
        ServerHttpResponse response = exchange.getResponse();
        
        log.error("Erreur d'authentification: {} - URI: {}", 
                authException.getMessage(), 
                exchange.getRequest().getURI());
        
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().toString());
        errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", "Authentification requise pour accéder à cette ressource");
        errorDetails.put("path", exchange.getRequest().getPath().toString());
        
        String exceptionMessage = authException.getMessage();
        String detailMessage = "Erreur d'authentification";
        
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("expired")) {
                detailMessage = "Token JWT expiré";
            } else if (exceptionMessage.contains("invalid")) {
                detailMessage = "Token JWT invalide";
            } else if (exceptionMessage.contains("malformed")) {
                detailMessage = "Format de token JWT incorrect";
            }
        }
        errorDetails.put("detail", detailMessage);
        
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorDetails);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation de la réponse JSON d'authentification", e);
            return Mono.error(e);
        }
    }
}
