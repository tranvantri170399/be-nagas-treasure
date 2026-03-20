package asia.rgp.game.nagas.modules.slot.domain.model;

import asia.rgp.game.nagas.shared.domain.model.Money;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SlotState implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String gameId;
    private String sessionId;
    private String parentRoundId;
    private int baseRoundNumber;
    private Money baseBet;

    // --- LOGIC FREE SPINS ---
    private int totalFreeSpins;
    private int remainingFreeSpins;
    
    private double accumulatedWin; 

    @Builder.Default
    @JsonProperty("is_hold_and_win")
    private Boolean holdAndWin = false; 

    @Builder.Default
    private Integer remainingRespins = 0;

    @Builder.Default
    private List<LockedBonus> lockedBonuses = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
        this.accumulatedWin += winAmount;
    }

    public void retrigger(int extraSpins) {
        this.totalFreeSpins += extraSpins;
        this.remainingFreeSpins += extraSpins;
    }

    public boolean isCellLocked(int r, int c) {
        if (this.lockedBonuses == null) return false;
        return this.lockedBonuses.stream()
                .anyMatch(b -> b.getRow() == r && b.getCol() == c);
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