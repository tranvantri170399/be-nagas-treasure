package asia.rgp.game.nagas.modules.slot.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class SlotHistory {
    private final String roundId;
    private final String parentRoundId;
    private final int round;
    private final int subRound;
    private final String userId;
    private final String gameId;
    private final String sessionId;
    private final long totalBet;
    private final long displayBet;
    private final long totalWin;
    private final long balanceAfter;
    private final int[][] screen;
    private final List<WinDetail> wins;
    private final String type;
    private final Instant createdAt;
}