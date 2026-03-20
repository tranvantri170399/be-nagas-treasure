package asia.rgp.game.nagas.modules.slot.domain.model;

import asia.rgp.game.nagas.shared.domain.model.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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

    public PayoutResult(Money totalWin, List<WinDetail> wins, boolean triggerFreeSpin, 
                        int freeSpinCount, int scatterSymbol, List<int[]> scatterPositions) {
        this.totalWin = totalWin;
        this.wins = wins;
        this.triggerFreeSpin = triggerFreeSpin;
        this.freeSpinCount = freeSpinCount;
        this.scatterSymbol = scatterSymbol;
        this.scatterPositions = scatterPositions;
        this.triggerHoldAndWin = false;
        this.bonusInfos = null;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class BonusInfo {
        private final int row;
        private final int col;
        private final int symbolId;
        private final double multiplier;
        private final String type;
    }

    public int getBonusCount() {
        return bonusInfos != null ? bonusInfos.size() : 0;
    }
}