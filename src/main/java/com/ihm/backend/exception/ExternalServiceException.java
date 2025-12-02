package cm.enspy.xccm.exception;

/**
 * Exception levée lors d'erreurs d'appel à des services externes
 */
public class ExternalServiceException extends TechnicalException {
    
    public ExternalServiceException(String serviceName, String message) {
        super(String.format("Erreur lors de l'appel au service %s: %s", serviceName, message));
    }
    
    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(String.format("Erreur lors de l'appel au service %s: %s", serviceName, message), cause);
    }
}