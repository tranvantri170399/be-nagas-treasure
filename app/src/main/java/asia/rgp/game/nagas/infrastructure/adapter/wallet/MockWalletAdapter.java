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

  public MockWalletAdapter() {
    userBalances.put("1", 10000000L);
  }

  @Override
  public long getBalance(String userId) {
    return userBalances.computeIfAbsent(userId, k -> 10000000L);
  }

  @Override
  public void debit(String userId, Money amount, String transactionId) {
    long currentBalance = getBalance(userId);
    long debitAmount = Math.round(amount.getAmount() * 100.0);

    log.info(
        "[MockWallet] DEBIT | User: {} | Amount (Real): {} | Amount (Internal): -{} | TX: {}",
        userId,
        amount.getAmount(),
        debitAmount,
        transactionId);

    if (currentBalance < debitAmount) {
      log.warn("[MockWallet] DEBIT FAILED | Insufficient balance for user {}", userId);
      throw new DomainException(
          "Số dư không đủ để thực hiện vòng quay", ErrorCode.INSUFFICIENT_BALANCE);
    }

    long newBalance = currentBalance - debitAmount;
    userBalances.put(userId, newBalance);
    log.info("[MockWallet] BALANCE UPDATED | User: {} | New Balance: {}", userId, newBalance);
  }

  @Override
  public void credit(String userId, Money amount, String transactionId) {
    long currentBalance = getBalance(userId);
    long creditAmount = Math.round(amount.getAmount() * 100.0);

    log.info(
        "[MockWallet] CREDIT | User: {} | Amount (Real): {} | Amount (Internal): +{} | TX: {}",
        userId,
        amount.getAmount(),
        creditAmount,
        transactionId);

    long newBalance = currentBalance + creditAmount;
    userBalances.put(userId, newBalance);
    log.info("[MockWallet] BALANCE UPDATED | User: {} | New Balance: {}", userId, newBalance);
  }
}
