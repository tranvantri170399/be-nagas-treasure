package asia.rgp.game.nagas.shared.application.exception;

import asia.rgp.game.nagas.shared.error.ErrorCode;

/**
 * Base exception for application layer errors. Represents use case errors, validation issues, and
 * application workflow errors.
 */
public class ApplicationException extends RuntimeException {

  private final ErrorCode errorCode;

  public ApplicationException(String message) {
    super(message);
    this.errorCode = ErrorCode.APPLICATION_ERROR;
  }

  public ApplicationException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = (errorCode != null ? errorCode : ErrorCode.APPLICATION_ERROR);
  }

  public ApplicationException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = ErrorCode.APPLICATION_ERROR;
  }

  public ApplicationException(String message, ErrorCode errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = (errorCode != null ? errorCode : ErrorCode.APPLICATION_ERROR);
  }

  public String getErrorCode() {
    return errorCode.name();
  }

  public ErrorCode getErrorCodeEnum() {
    return errorCode;
  }
}
