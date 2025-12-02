package cm.enspy.xccm.exception;

/**
 * Exception levée quand un email est déjà utilisé
 */
public class EmailAlreadyExistsException extends DuplicateResourceException {
    
    public EmailAlreadyExistsException(String email) {
        super("Utilisateur", "email", email);
    }
}


