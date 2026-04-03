package asia.rgp.game.nagas.shared.presentation.api;

/**
 * Standard error constants for API error responses. Only keeps the error **codes** as String
 * constants to keep things simple.
 *
 * <p>Example usage: - ErrorConstant.DOMAIN_ERROR - ErrorConstant.VALIDATION_FAILED
 */
public final class ErrorConstant {

  private ErrorConstant() {
    // Utility class - prevent instantiation
  }

  // ==================== Domain Layer Errors (1xxx) ====================
  // 1xxx – domain-level business rule violations
  public static final String DOMAIN_ERROR = "1000";

  // ==================== Application Layer Errors (2xxx) ====================
  // 2xxx – application orchestration / use-case errors
  public static final String APPLICATION_ERROR = "2000";
  public static final String DUPLICATE_ENTRY = "2001";

  // ==================== Infrastructure Layer Errors (3xxx) ====================
  // 3xxx – technical / infrastructure-level errors
  public static final String DATABASE_ERROR = "3000";
  public static final String ENTITY_NOT_FOUND = "3001";
  public static final String DATA_INTEGRITY_VIOLATION = "3002";
  public static final String EXTERNAL_SERVICE_ERROR = "3003";

  // ==================== HTTP Layer Errors (4xx) ====================
  // 400 Bad Request – generic client error
  public static final String MISSING_PARAMETER = "400";
  public static final String TYPE_MISMATCH = "400";
  public static final String INVALID_REQUEST_BODY = "400";

  // 404 Not Found
  public static final String NOT_FOUND = "404";

  // 422 Unprocessable Content – request is syntactically correct but semantically invalid
  // (validation)
  public static final String VALIDATION_FAILED = "422";

  // ==================== Server Errors (5xx) ====================
  // 500 Internal Server Error
  public static final String INTERNAL_SERVER_ERROR = "500";
}
