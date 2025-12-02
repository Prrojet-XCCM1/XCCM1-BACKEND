// ==================== EXCEPTIONS 400 - BAD REQUEST ====================

package cm.enspy.xccm.exception;

/**
 * Exception levée quand les données fournies sont invalides
 */
public class BadRequestException extends BusinessException {
    
    public BadRequestException(String message) {
        super(message, "BAD_REQUEST", 400);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, "BAD_REQUEST", 400, cause);
    }
}

