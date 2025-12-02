package cm.enspy.xccm.exception;

import lombok.Getter;
import java.util.Map;

/**
 * Exception levée lors d'erreurs de validation
 */
@Getter
public class ValidationException extends BusinessException {
    
    private final Map<String, String> validationErrors;
    
    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message, "VALIDATION_ERROR", 400);
        this.validationErrors = validationErrors;
    }
}


