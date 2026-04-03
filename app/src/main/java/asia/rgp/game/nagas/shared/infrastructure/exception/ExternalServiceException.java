package asia.rgp.game.nagas.shared.infrastructure.exception;

import asia.rgp.game.nagas.shared.error.ErrorCode;

/**
 * Base exception for external service layer errors. Represents HTTP client errors, service
 * connection issues, and external API failures.
 */
public class ExternalServiceException extends RuntimeException {

  private final ErrorCode errorCode;

  public ExternalServiceException(String message) {
    super(message);
    this.errorCode = ErrorCode.EXTERNAL_SERVICE_ERROR;
  }

  public ExternalServiceException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = (errorCode != null ? errorCode : ErrorCode.EXTERNAL_SERVICE_ERROR);
  }

  public ExternalServiceException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = ErrorCode.EXTERNAL_SERVICE_ERROR;
  }

  public ExternalServiceException(String message, ErrorCode errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = (errorCode != null ? errorCode : ErrorCode.EXTERNAL_SERVICE_ERROR);
  }

  public String getErrorCode() {
    return errorCode != null ? errorCode.name() : null;
  }

  public ErrorCode getErrorCodeEnum() {
    return errorCode;
  }
}
