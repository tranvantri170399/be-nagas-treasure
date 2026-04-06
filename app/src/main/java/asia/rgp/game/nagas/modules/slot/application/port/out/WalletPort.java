package asia.rgp.game.nagas.modules.slot.application.port.out;

import asia.rgp.game.nagas.shared.domain.model.Money;

/**
 * [PORT OUT] Interface for communicating with the external Wallet Service. Ensures the Slot module
 * remains decoupled from specific communication protocols (gRPC/REST).
 */
public interface WalletPort {

  long getBalance(String agentId, String userId);

  void debit(String agentId, String userId, Money amount, String transactionId);

  void credit(String agentId, String userId, Money amount, String transactionId);
}
