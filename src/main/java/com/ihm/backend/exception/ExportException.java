package cm.enspy.xccm.exception;

/**
 * Exception personnalisée pour les erreurs d'export de documents
 * Utilisée par PdfGenerator et DocxGenerator
 *
 */
public class ExportException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructeur avec message d'erreur
     * @param message Description de l'erreur
     */
    public ExportException(String message) {
        super(message);
    }

    /**
     * Constructeur avec message et cause
     * @param message Description de l'erreur
     * @param cause Exception d'origine
     */
    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructeur avec cause uniquement
     * @param cause Exception d'origine
     */
    public ExportException(Throwable cause) {
        super(cause);
    }
}