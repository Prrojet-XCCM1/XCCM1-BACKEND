package cm.enspy.xccm.exception;

/**
 * Exception levée quand l'utilisateur tente d'accéder à une ressource qui ne lui appartient pas
 */
public class AccessDeniedException extends BusinessException {
    
    public AccessDeniedException(String message) {
        super(message, "ACCESS_DENIED", 403);
    }
}


