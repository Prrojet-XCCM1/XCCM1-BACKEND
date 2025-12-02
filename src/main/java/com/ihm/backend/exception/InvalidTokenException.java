package cm.enspy.xccm.exception;

/**
 * Exception levée quand le token JWT est invalide ou expiré
 */
public class InvalidTokenException extends BusinessException {
    
    public InvalidTokenException(String message) {
        super(message, "INVALID_TOKEN", 401);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, "INVALID_TOKEN", 401, cause);
    }
}


