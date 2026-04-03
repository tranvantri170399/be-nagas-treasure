package asia.rgp.game.nagas.modules.slot.domain.model;

import asia.rgp.game.nagas.shared.domain.model.Money;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PayoutResult {
  private final Money totalWin;
  private final List<WinDetail> wins;

  private final boolean triggerFreeSpin;
  private final int freeSpinCount;
  private final int scatterSymbol;
  private final List<int[]> scatterPositions;

  // HOLD AND WIN FIELDS
  private final boolean triggerHoldAndWin;
  private final List<BonusInfo> bonusInfos;

  // --- PROGRESSIVE JACKPOT FIELDS ---
  private final boolean jackpotTriggered;
  private final List<int[]> glowingRingPositions;

  public static PayoutResult empty() {
    return PayoutResult.builder()
        .totalWin(Money.zero())
        .wins(Collections.emptyList())
        .triggerFreeSpin(false)
        .freeSpinCount(0)
        .scatterSymbol(0)
        .scatterPositions(Collections.emptyList())
        .triggerHoldAndWin(false)
        .bonusInfos(Collections.emptyList())
        .jackpotTriggered(false)
        .glowingRingPositions(Collections.emptyList())
        .build();
  }

  public PayoutResult(
      Money totalWin,
      List<WinDetail> wins,
      boolean triggerFreeSpin,
      int freeSpinCount,
      int scatterSymbol,
      List<int[]> scatterPositions) {
    this.totalWin = totalWin;
    this.wins = wins;
    this.triggerFreeSpin = triggerFreeSpin;
    this.freeSpinCount = freeSpinCount;
    this.scatterSymbol = scatterSymbol;
    this.scatterPositions = scatterPositions;
    this.triggerHoldAndWin = false;
    this.bonusInfos = Collections.emptyList();
    this.jackpotTriggered = false;
    this.glowingRingPositions = Collections.emptyList();
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class BonusInfo {
    private final int row;
    private final int col;
    private final int symbolId;
    private final double multiplier;
    private final String type; // CASH, MINI, MAJOR
  }

  public int getBonusCount() {
    return bonusInfos != null ? bonusInfos.size() : 0;
  }

  public int getGlowingRingCount() {
    return glowingRingPositions != null ? glowingRingPositions.size() : 0;
  }
}
