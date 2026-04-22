package asia.rgp.game.nagas.modules.slot.domain.service;

import asia.rgp.game.nagas.shared.domain.model.Money;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

public interface JackpotService {
  void contribute(String agencyId, Money amount);

  Map<String, Double> getAllPools(String agencyId);

  JackpotSpinResult spinWheel(String agencyId, String userId, String sessionId, Money currentBet);

  void markPaid(String winId);

  void markFailed(String winId, String errorMessage);

  @Getter
  @Builder
  class JackpotSpinResult {
    private final String winId;
    private final String tierName;
    private final Money amount;
    private final boolean hitArrow;
    private final boolean nearMiss;
  }
}
