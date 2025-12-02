package cm.enspy.xccm.exception;

import cm.enspy.xccm.domain.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
// Import WebExchangeBindException pour WebFlux
import org.springframework.web.bind.support.WebExchangeBindException; 
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
// WebRequest n'est plus utilisé
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
// NoHandlerFoundException (Servlet) n'est plus utilisé dans WebFlux

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions (compatible WebFlux)
 * Intercepte toutes les exceptions et retourne des réponses ApiResponse standardisées
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==================== EXCEPTIONS MÉTIER ====================

    /**
     * Gère toutes les exceptions métier personnalisées
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex
    ) {
        log.warn("Exception métier: {} - Code: {}", ex.getMessage(), ex.getErrorCode());
        
        ApiResponse<Void> response = ApiResponse.error(
                ex.getHttpStatus(),
                ex.getMessage(),
                ex.getErrorCode()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Gère les exceptions de validation avec détails des champs
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            ValidationException ex
    ) {
        log.warn("Erreur de validation: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.validationError(
                "Erreur de validation des données",
                ex.getValidationErrors()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Gère ResourceNotFoundException (404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex
    ) {
        log.warn("Ressource introuvable: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.notFound(
                "Ressource introuvable",
                ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Gère DuplicateResourceException (409)
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(
            DuplicateResourceException ex
    ) {
        log.warn("Ressource dupliquée: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.conflict(
                "Conflit de ressource",
                ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Gère UnauthorizedException (401)
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(
            UnauthorizedException ex
    ) {
        log.warn("Non autorisé: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.unauthorized(
                "Authentification requise",
                ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Gère ForbiddenException (403)
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbiddenException(
            ForbiddenException ex
    ) {
        log.warn("Accès refusé: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.forbidden(
                "Accès refusé",
                ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ==================== EXCEPTIONS SPRING SECURITY ====================

    /**
     * Gère les erreurs d'authentification Spring Security
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex
    ) {
        log.error("Erreur d'authentification: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.unauthorized(
                "Authentification échouée",
                "Identifiants invalides ou session expirée"
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Gère BadCredentialsException (identifiants invalides)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex
    ) {
        log.warn("Identifiants invalides");
        
        ApiResponse<Void> response = ApiResponse.unauthorized(
                "Identifiants invalides",
                "L'email ou le mot de passe est incorrect"
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Gère AccessDeniedException Spring Security
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex
    ) {
        log.warn("Accès refusé: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.forbidden(
                "Accès refusé",
                "Vous n'avez pas les permissions nécessaires pour effectuer cette action"
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ==================== EXCEPTIONS DE VALIDATION ====================

    /**
     * Gère les erreurs de validation @Valid sur les méthodes de contrôleur (WebFlux)
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebExchangeBindException(
            WebExchangeBindException ex
    ) {
        log.warn("Erreur de validation des arguments (WebFlux)");
        
        Map<String, String> errors = new HashMap<>();
        // getBindingResult() fonctionne de la même manière pour extraire les erreurs
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Void> response = ApiResponse.validationError(
                "Erreur de validation des données",
                errors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Gère les violations de contraintes de validation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex
    ) {
        log.warn("Violation de contrainte de validation");
        
        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));
        
        ApiResponse<Void> response = ApiResponse.validationError(
                "Erreur de validation des contraintes",
                errors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== AUTRES EXCEPTIONS ====================

    /**
     * Gère les erreurs de type d'argument
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        log.warn("Type d'argument invalide: {} - Attendu: {}", 
                ex.getValue(), ex.getRequiredType());
        
        String message = String.format(
                "Le paramètre '%s' a une valeur invalide '%s'. Type attendu: %s",
                ex.getName(),
                ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );
        
        ApiResponse<Void> response = ApiResponse.badRequest(
                "Paramètre invalide",
                message
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    // NOTE: handleNoHandlerFoundException est retiré car NoHandlerFoundException est spécifique à Spring MVC.
    // Les erreurs 404 dans WebFlux sont généralement gérées par le pipeline en amont.

    /**
     * Gère IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {
        log.warn("Argument illégal: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.badRequest(
                "Argument invalide",
                ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Gère IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(
            IllegalStateException ex
    ) {
        log.warn("État illégal: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.badRequest(
                "Opération invalide",
                ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== EXCEPTION GÉNÉRIQUE ====================

    /**
     * Gère toutes les autres exceptions non prévues
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex
    ) {
        log.error("Erreur interne du serveur", ex);
        
        // Ne pas exposer les détails techniques en production
        String errorMessage = "Une erreur interne est survenue. Veuillez réessayer plus tard.";
        
        // En développement, on peut exposer plus de détails
        if (isDevelopmentMode()) {
            errorMessage = ex.getMessage();
        }
        
        ApiResponse<Void> response = ApiResponse.internalError(
                "Erreur interne du serveur",
                errorMessage
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Vérifie si l'application est en mode développement
     */
    private boolean isDevelopmentMode() {
        String activeProfile = System.getProperty("spring.profiles.active", "");
        return activeProfile.contains("dev") || activeProfile.contains("local");
    }
}

