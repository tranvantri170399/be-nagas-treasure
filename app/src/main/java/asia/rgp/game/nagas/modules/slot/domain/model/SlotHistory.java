package asia.rgp.game.nagas.modules.slot.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SlotHistory {
  private final String roundId;
  private final String parentRoundId;
  private final String agentId;
  private final String userId;
  private final String gameId;
  private final String sessionId;

  // Mode
  private final String thisMode;
  private final String nextMode;

  // Financials
  private final BigDecimal betAmount;
  private final BigDecimal totalWin;
  private final boolean trialMode;

  // Game data
  private final int[][] screen;
  private final List<WinDetail> wins;

  // Bonus state snapshot
  private final Integer freeSpinsTotal;
  private final Integer freeSpinsRemain;
  private final Integer respinsRemain;

  // Jackpot
  private final String jackpotWonTier;
  private final BigDecimal jackpotWonAmount;
  private final BigDecimal jackpotContribution;

  private final Instant timestamp;
}
