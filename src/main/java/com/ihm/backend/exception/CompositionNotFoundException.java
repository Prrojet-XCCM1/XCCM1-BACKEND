package cm.enspy.xccm.exception;

/**
 * Exception levée quand une composition n'est pas trouvée
 */
public class CompositionNotFoundException extends ResourceNotFoundException {
    
    public CompositionNotFoundException(java.util.UUID id) {
        super("Composition", id);
    }
}


