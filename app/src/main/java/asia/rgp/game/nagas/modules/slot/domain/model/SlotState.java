package asia.rgp.game.nagas.modules.slot.domain.model;

import asia.rgp.game.nagas.shared.domain.model.Money;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlotState implements Serializable {

  private static final long serialVersionUID = 1L;

  private String agencyId;
  private String userId;
  private String gameId;
  private String sessionId;

  // --- TRACKING ID ---
  private String parentRoundId;
  private String lastRoundId;
  private String triggerRoundId;
  private int baseRoundNumber;
  private Money baseBet;

  // --- SNAPSHOT ---
  private int[][] lastGrid;

  // --- LOGIC FREE SPINS ---
  private int totalFreeSpins;
  private int remainingFreeSpins;

  @Builder.Default
  @JsonProperty("is_free_spin")
  private Boolean freeSpinMode = false;

  private double accumulatedWin;

  // --- LOGIC HOLD AND WIN ---
  @Builder.Default
  @JsonProperty("is_hold_and_win")
  private Boolean holdAndWin = false;

  @Builder.Default private Integer remainingRespins = 0;

  @Builder.Default private List<LockedBonus> lockedBonuses = new ArrayList<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class LockedBonus implements Serializable {
    private int row;
    private int col;
    private int symbolId;
    private double multiplier;
    private String type;
  }

  // --- HELPER METHODS ---
  public boolean isHoldAndWinMode() {
    return Boolean.TRUE.equals(holdAndWin);
  }

  public boolean isFreeSpinMode() {
    return remainingFreeSpins > 0;
  }

  public void consumeFreeSpin() {
    if (this.remainingFreeSpins > 0) this.remainingFreeSpins--;
  }

  public void addWin(double winAmount) {
    this.accumulatedWin = Math.round((this.accumulatedWin + winAmount) * 100.0) / 100.0;
  }

  public void retrigger(int extraSpins) {
    this.totalFreeSpins += extraSpins;
    this.remainingFreeSpins += extraSpins;
  }

  public boolean isCellLocked(int r, int c) {
    if (this.lockedBonuses == null) return false;
    return this.lockedBonuses.stream().anyMatch(b -> b.getRow() == r && b.getCol() == c);
  }

  public void addLockedBonus(LockedBonus bonus) {
    if (this.lockedBonuses == null) {
      this.lockedBonuses = new ArrayList<>();
    }
    this.lockedBonuses.add(bonus);
  }

  public boolean isFullGrid() {
    return this.lockedBonuses != null && this.lockedBonuses.size() >= 15;
  }
}
