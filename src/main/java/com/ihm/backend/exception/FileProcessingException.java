package cm.enspy.xccm.exception;

/**
 * Exception levée lors d'erreurs de traitement de fichiers
 */
public class FileProcessingException extends TechnicalException {
    
    public FileProcessingException(String message) {
        super(message);
    }
    
    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}