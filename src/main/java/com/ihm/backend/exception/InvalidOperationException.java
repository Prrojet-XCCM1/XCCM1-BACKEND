package cm.enspy.xccm.exception;

/**
 * Exception levée lors d'un conflit d'état
 */
public class InvalidOperationException extends BusinessException {
    
    public InvalidOperationException(String message) {
        super(message, "INVALID_OPERATION", 409);
    }
    
    public InvalidOperationException(String message, Throwable cause) {
        super(message, "INVALID_OPERATION", 409, cause);
    }
}

