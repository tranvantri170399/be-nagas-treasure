package asia.rgp.game.nagas.shared.error;

/** System-wide error codes. Categorized by layer and specific Slot Game business logic. */
public enum ErrorCode {

  // ===== 1. Generic Layer-level Errors =====
  DOMAIN_ERROR, // Business rule violation in the Domain layer
  APPLICATION_ERROR, // Coordination failure in the Application layer
  INFRASTRUCTURE_ERROR, // Technical connection failure (DB, Redis, Message Queue)
  DATABASE_ERROR,
  INTERNAL_SERVER_ERROR,

  // ===== 2. Validation & Request Errors =====
  VALIDATION_ERROR,
  MISSING_PARAMETER,
  TYPE_MISMATCH,
  INVALID_REQUEST_BODY,
  INVALID_PATTERN,
  INVALID_ARGUMENT,

  // ===== 3. Resource & Entity Errors =====
  NOT_FOUND,
  ENTITY_NOT_FOUND,
  DATA_INTEGRITY_VIOLATION,
  CONFLICT,
  DUPLICATE_ENTRY,

  // ===== 4. Auth & Security =====
  UNAUTHORIZED,
  FORBIDDEN,
  INVALID_SIGNATURE, // Invalid signature for inter-microservice calls
  TOKEN_EXPIRED,

  // ===== 5. Game Configuration Errors =====
  GAME_NOT_FOUND, // Game ID not found
  GAME_NOT_ACTIVE, // Game is under maintenance or closed
  SYMBOL_NOT_FOUND,
  SYMBOL_CONFIG_EMPTY,
  REGULAR_SYMBOL_CONFIG_EMPTY,
  SYMBOL_PAYTABLE_NOT_CONFIGURED, // Paytable has not been configured
  SYMBOL_PAYOUT_VALUE_NOT_FOUND, // Missing payout value for a specific match count
  SYMBOL_CACHE_REFRESH_FAILED,
  INVALID_DEFAULT_REEL_COUNT,
  INVALID_DEFAULT_ROWS_PER_REEL,
  INVALID_DEFAULT_REEL_STRIP_COUNT,
  INVALID_PAYLINE_CONFIG, // Payline configuration has invalid coordinates
  RTP_CONFIG_INVALID, // Invalid Return To Player (RTP) configuration

  // ===== 6. Bet & Wallet Errors =====
  INVALID_BET_AMOUNT, // Bet amount is not in the allowed list
  BET_AMOUNT_TOO_LOW,
  BET_AMOUNT_TOO_HIGH,
  INVALID_CURRENCY, // Currency type not supported
  BALANCE_NOT_ENOUGH, // Insufficient balance (Internal check)
  INSUFFICIENT_BALANCE, // Insufficient balance (Returned from Wallet Service)
  BALANCE_SERVICE_ERROR,
  BALANCE_SERVICE_UNAVAILABLE,
  BALANCE_OPERATION_FAILED,
  TRANSACTION_ALREADY_PROCESSED, // Idempotency check (Transaction already handled)
  TRANSACTION_FAILED,

  // ===== 7. Session & Game Flow Errors =====
  SESSION_NOT_FOUND,
  SESSION_NOT_ACTIVE,
  SESSION_MODE_MISMATCH,
  INVALID_GAME_MODE,
  FREE_GAME_ACTIVE,
  FREE_GAME_NOT_ACTIVE,
  RESPIN_ACTIVE,
  RESPIN_NOT_ACTIVE,
  JACKPOT_NOT_AVAILABLE,
  JACKPOT_NOT_ACTIVE,
  JACKPOT_BONUS_SESSION_MISMATCH,
  LOCK_ACQUISITION_FAILED, // Failed to lock User (preventing spam spins)

  // ===== 8. Spin Result & Calculation Validation =====
  SPIN_RESULT_USER_ID_NULL,
  SPIN_RESULT_BET_AMOUNT_INVALID,
  SPIN_RESULT_REELS_NULL_OR_EMPTY,
  SPIN_RESULT_REEL_COUNT_INVALID,
  SPIN_RESULT_ROWS_PER_REEL_INVALID,
  SPIN_RESULT_REELS_SIZE_MISMATCH,
  SPIN_RESULT_REEL_ROWS_INVALID,
  SPIN_RESULT_REELS_CONTAINS_INVALID_SYMBOL,
  SPIN_RESULT_CREATED_AT_NULL,
  MAX_WIN_EXCEEDED, // Exceeded maximum allowable win (Safety limit)

  // ===== 9. External Service Errors =====
  EXTERNAL_SERVICE_ERROR,
  EXTERNAL_SERVICE_UNAVAILABLE,
  EXTERNAL_API_TIMEOUT,
  EXTERNAL_API_BAD_GATEWAY;

  public String code() {
    return name();
  }
}
