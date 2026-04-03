package asia.rgp.game.nagas.modules.slot.application.port.out;

import asia.rgp.game.nagas.shared.domain.model.Money;

/**
 * [PORT OUT] Interface for communicating with the external Wallet Service. Ensures the Slot module
 * remains decoupled from specific communication protocols (gRPC/REST).
 */
public interface WalletPort {

  /**
   * Retrieves the current balance of the player.
   *
   * @param userId Unique identifier of the player (String to support UUID/External IDs).
   * @return The current balance in the smallest currency unit (long).
   */
  long getBalance(String userId);

  /**
   * Calls external service to deduct bet amount.
   *
   * @param userId Unique identifier of the player.
   * @param amount The bet amount.
   * @param transactionId Unique ID for idempotency (Mapping to roundId).
   */
  void debit(String userId, Money amount, String transactionId);

  /**
   * Calls external service to credit win amount.
   *
   * @param userId Unique identifier of the player.
   * @param amount The total win amount.
   * @param transactionId Unique ID for idempotency.
   */
  void credit(String userId, Money amount, String transactionId);
}
