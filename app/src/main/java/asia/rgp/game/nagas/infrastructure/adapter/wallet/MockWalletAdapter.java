package asia.rgp.game.nagas.infrastructure.adapter.wallet;

import asia.rgp.game.nagas.modules.slot.application.port.out.WalletPort;
import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.domain.model.Money;
import asia.rgp.game.nagas.shared.error.ErrorCode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockWalletAdapter implements WalletPort {
  private final Map<String, Long> userBalances = new ConcurrentHashMap<>();

  private String walletKey(String agencyId, String userId) {
    return agencyId + ":" + userId;
  }

  @Override
  public long getBalance(String agencyId, String userId) {
    return userBalances.computeIfAbsent(walletKey(agencyId, userId), k -> 10000000L);
  }

  @Override
  public void debit(String agencyId, String userId, Money amount, String transactionId) {
    String key = walletKey(agencyId, userId);
    long currentBalance = getBalance(agencyId, userId);
    long debitAmount = Math.round(amount.getAmount() * 100.0);

    log.info(
        "[MockWallet] DEBIT | Agent: {} | User: {} | Amount: -{} | TX: {}",
        agencyId,
        userId,
        debitAmount,
        transactionId);

    if (currentBalance < debitAmount) {
      throw new DomainException("Insufficient balance for spin", ErrorCode.INSUFFICIENT_BALANCE);
    }

    long newBalance = currentBalance - debitAmount;
    userBalances.put(key, newBalance);
  }

  @Override
  public void credit(String agencyId, String userId, Money amount, String transactionId) {
    String key = walletKey(agencyId, userId);
    long currentBalance = getBalance(agencyId, userId);
    long creditAmount = Math.round(amount.getAmount() * 100.0);

    log.info(
        "[MockWallet] CREDIT | Agent: {} | User: {} | Amount: +{} | TX: {}",
        agencyId,
        userId,
        creditAmount,
        transactionId);

    long newBalance = currentBalance + creditAmount;
    userBalances.put(key, newBalance);
  }
}
