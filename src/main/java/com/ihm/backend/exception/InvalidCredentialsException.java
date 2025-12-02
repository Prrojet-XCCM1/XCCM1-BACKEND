package cm.enspy.xccm.exception;

/**
 * Exception levée quand les credentials sont invalides
 */
public class InvalidCredentialsException extends BusinessException {
    
    public InvalidCredentialsException(String message) {
        super(message, "INVALID_CREDENTIALS", 401);
    }
}


