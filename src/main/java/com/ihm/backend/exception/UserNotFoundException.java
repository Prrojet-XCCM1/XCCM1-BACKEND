package cm.enspy.xccm.exception;

/**
 * Exception levée quand un utilisateur n'est pas trouvé
 */
public class UserNotFoundException extends ResourceNotFoundException {
    
    public UserNotFoundException(String email) {
        super("Utilisateur", email);
    }
}


