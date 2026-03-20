package asia.rgp.game.nagas.shared.domain.exception;

import asia.rgp.game.nagas.shared.error.ErrorCode;

/**
 * Base exception for domain layer errors. Represents business rule violations and domain logic
 * errors.
 */
public class DomainException extends RuntimeException {

  private final ErrorCode errorCode;

  public DomainException(String message) {
    super(message);
    this.errorCode = ErrorCode.DOMAIN_ERROR;
  }

  public DomainException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = (errorCode != null ? errorCode : ErrorCode.DOMAIN_ERROR);
  }

  public DomainException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = ErrorCode.DOMAIN_ERROR;
  }

  public DomainException(String message, ErrorCode errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = (errorCode != null ? errorCode : ErrorCode.DOMAIN_ERROR);
  }

  public String getErrorCode() {
    return errorCode.name();
  }

  public ErrorCode getErrorCodeEnum() {
    return errorCode;
  }
}
