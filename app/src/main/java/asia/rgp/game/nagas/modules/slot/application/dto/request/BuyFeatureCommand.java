package asia.rgp.game.nagas.modules.slot.application.dto.request;

import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.shared.domain.model.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuyFeatureCommand {

  private final String agentId;

  private final String userId;

  private final String gameId;

  private final String sessionId;

  private final String featureName;

  private final Money betAmount;

  private final boolean trialMode;

  public Money calculateTotalCost(double multiplier) {
    if (betAmount == null) return Money.zero();
    return betAmount.times(multiplier);
  }

  public boolean isBuyHoldAndWin() {
    return SlotConstants.FEATURE_HOLD_AND_WIN.equalsIgnoreCase(featureName);
  }

  public boolean isBuyFreeSpins() {
    return SlotConstants.FEATURE_FREE_SPINS.equalsIgnoreCase(featureName);
  }
}
