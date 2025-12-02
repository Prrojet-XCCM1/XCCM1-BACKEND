// ==================== EXCEPTIONS 401 - UNAUTHORIZED ====================

package cm.enspy.xccm.exception;

/**
 * Exception levée quand l'authentification est requise ou a échoué
 */
public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", 401);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, "UNAUTHORIZED", 401, cause);
    }
}


