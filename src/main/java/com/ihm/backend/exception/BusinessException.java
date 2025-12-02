// ==================== EXCEPTION DE BASE ====================

package cm.enspy.xccm.exception;

import lombok.Getter;

/**
 * Classe de base pour toutes les exceptions métier
 */
@Getter
public abstract class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;
    
    public BusinessException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public BusinessException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
