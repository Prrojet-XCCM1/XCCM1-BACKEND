
// ==================== EXCEPTIONS 500 - INTERNAL SERVER ERROR ====================

package cm.enspy.xccm.exception;

/**
 * Exception levée lors d'erreurs techniques internes
 */
public class TechnicalException extends BusinessException {
    
    public TechnicalException(String message) {
        super(message, "TECHNICAL_ERROR", 500);
    }
    
    public TechnicalException(String message, Throwable cause) {
        super(message, "TECHNICAL_ERROR", 500, cause);
    }
}


