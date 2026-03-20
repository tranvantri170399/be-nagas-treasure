package asia.rgp.game.nagas.shared.infrastructure.exception;

import asia.rgp.game.nagas.shared.presentation.api.ErrorConstant;

/**
 * Exception thrown when database integrity constraints are violated.
 * Examples: unique constraint violations, foreign key violations.
 */
public class DataIntegrityViolationException extends DatabaseException {

    public DataIntegrityViolationException(String message) {
        super(message, ErrorConstant.DATA_INTEGRITY_VIOLATION);
    }

    public DataIntegrityViolationException(String message, Throwable cause) {
        super(message, ErrorConstant.DATA_INTEGRITY_VIOLATION, cause);
    }
}

