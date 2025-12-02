package cm.enspy.xccm.exception;

/**
 * Exception levée lors d'erreurs de base de données
 */
public class DatabaseException extends TechnicalException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}





