// ==================== EXCEPTIONS 403 - FORBIDDEN ====================

package cm.enspy.xccm.exception;

/**
 * Exception levée quand l'utilisateur n'a pas les permissions nécessaires
 */
public class ForbiddenException extends BusinessException {
    
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", 403);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(message, "FORBIDDEN", 403, cause);
    }
}


