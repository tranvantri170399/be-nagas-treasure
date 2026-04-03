package asia.rgp.game.nagas.modules.slot.domain.service;

import asia.rgp.game.nagas.shared.domain.model.Money;
import java.util.Map;
import lombok.Value;

public interface JackpotService {
  void contribute(Money amount);

  Map<String, Double> getAllPools();

  JackpotSpinResult spinWheel(Money currentBet);

  @Value
  class JackpotSpinResult {
    String tierName;
    Money amount;
    boolean hitArrow;
    boolean isNearMiss;
  }
}
