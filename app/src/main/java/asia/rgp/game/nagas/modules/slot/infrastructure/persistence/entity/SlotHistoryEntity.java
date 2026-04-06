package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity;

import asia.rgp.game.nagas.shared.infrastructure.persistence.document.BaseDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "spin_history")
@CompoundIndex(name = "idx_agent_user_ts", def = "{'agent_id': 1, 'user_id': 1, 'timestamp': -1}")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotHistoryEntity extends BaseDocument {

  // Tenant
  @Field("agent_id")
  private String agentId;

  @Field("user_id")
  private String userId;

  @Field("game_id")
  private String gameId;

  @Field("session_id")
  private String sessionId;

  // Round tracking
  @Field("round_id")
  private String roundId;

  @Field("parent_round_id")
  private String parentRoundId;

  // Mode
  @Field("this_mode")
  private String thisMode;

  @Field("next_mode")
  private String nextMode;

  // Financials
  @Field("bet_amount")
  private BigDecimal betAmount;

  @Field("total_win")
  private BigDecimal totalWin;

  @Field("trial_mode")
  private boolean trialMode;

  // Game data
  private int[][] screen;

  private List<WinLineRecord> wins;

  // Bonus state snapshot
  @Field("free_spins_total")
  private Integer freeSpinsTotal;

  @Field("free_spins_remain")
  private Integer freeSpinsRemain;

  @Field("respins_remain")
  private Integer respinsRemain;

  // Jackpot
  @Field("jackpot_won_tier")
  private String jackpotWonTier;

  @Field("jackpot_won_amount")
  private BigDecimal jackpotWonAmount;

  @Field("jackpot_contribution")
  private BigDecimal jackpotContribution;

  private Instant timestamp;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WinLineRecord {
    private int payline;
    private int symbol;
    private int occurs;
    private BigDecimal win;
    private String type;
  }
}
