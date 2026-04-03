package asia.rgp.game.nagas.shared.presentation.exception;

import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.error.ErrorCode;
import asia.rgp.game.nagas.shared.presentation.api.ErrorConstant;
import asia.rgp.game.nagas.shared.presentation.dto.ErrorResponse;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
    ErrorCode errorCode = ex.getErrorCodeEnum();
    String apiCode = mapToApiCode(errorCode);

    log.warn("[DOMAIN ERROR] API_CODE: {}, Message: {}", apiCode, ex.getMessage());

    ErrorResponse error =
        ErrorResponse.builder()
            .code(apiCode)
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

    return new ResponseEntity<>(error, mapToHttpStatus(errorCode));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
    log.error("[SYSTEM CRITICAL ERROR] ", ex);

    ErrorResponse error =
        ErrorResponse.builder()
            .code(ErrorConstant.INTERNAL_SERVER_ERROR)
            .message("An unexpected system error occurred.")
            .timestamp(LocalDateTime.now())
            .build();

    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private String mapToApiCode(ErrorCode code) {
    if (code == null) return ErrorConstant.DOMAIN_ERROR;

    return switch (code) {
      case INVALID_ARGUMENT, VALIDATION_ERROR -> ErrorConstant.VALIDATION_FAILED;
      case ENTITY_NOT_FOUND, GAME_NOT_FOUND -> ErrorConstant.NOT_FOUND;
      case DATABASE_ERROR -> ErrorConstant.DATABASE_ERROR;
      case DUPLICATE_ENTRY -> ErrorConstant.DUPLICATE_ENTRY;
      default -> ErrorConstant.DOMAIN_ERROR;
    };
  }

  private HttpStatus mapToHttpStatus(ErrorCode code) {
    if (code == null) return HttpStatus.BAD_REQUEST;

    return switch (code) {
      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      case NOT_FOUND, GAME_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case INVALID_ARGUMENT, VALIDATION_ERROR -> HttpStatus.UNPROCESSABLE_ENTITY;
      case INSUFFICIENT_BALANCE -> HttpStatus.PAYMENT_REQUIRED;
      default -> HttpStatus.BAD_REQUEST;
    };
  }
}
