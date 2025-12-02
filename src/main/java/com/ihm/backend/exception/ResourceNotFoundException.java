// ==================== EXCEPTIONS 404 - NOT FOUND ====================

package cm.enspy.xccm.exception;

/**
 * Exception levée quand une ressource n'est pas trouvée
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(
            String.format("%s introuvable avec l'identifiant: %s", resourceName, identifier),
            "RESOURCE_NOT_FOUND",
            404
        );
    }
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", 404);
    }
}


