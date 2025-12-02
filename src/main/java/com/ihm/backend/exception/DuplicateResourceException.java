// ==================== EXCEPTIONS 409 - CONFLICT ====================

package cm.enspy.xccm.exception;

/**
 * Exception levée quand une ressource existe déjà
 */
public class DuplicateResourceException extends BusinessException {
    
    public DuplicateResourceException(String resourceName, String field, Object value) {
        super(
            String.format("%s avec %s '%s' existe déjà", resourceName, field, value),
            "DUPLICATE_RESOURCE",
            409
        );
    }
    
    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE", 409);
    }
}


