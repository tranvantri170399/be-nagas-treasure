package asia.rgp.game.nagas.shared.infrastructure.exception;

/**
 * Base exception for database layer errors.
 * Represents database connection issues, query failures, and data access
 * errors.
 */
public class DatabaseException extends RuntimeException {

    private final String errorCode;

    public DatabaseException(String message) {
        super(message);
        this.errorCode = null;
    }

    public DatabaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public DatabaseException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
