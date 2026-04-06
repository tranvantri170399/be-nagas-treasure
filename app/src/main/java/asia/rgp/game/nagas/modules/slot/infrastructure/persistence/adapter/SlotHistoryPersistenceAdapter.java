package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.adapter;

import asia.rgp.game.nagas.modules.slot.application.port.out.SlotHistoryPort;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotHistory;
import asia.rgp.game.nagas.modules.slot.domain.model.WinDetail;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.SlotHistoryEntity;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository.MongoSlotHistoryRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlotHistoryPersistenceAdapter implements SlotHistoryPort {

  private final MongoSlotHistoryRepository repository;

  @Override
  public void save(SlotHistory history) {
    SlotHistoryEntity entity =
        SlotHistoryEntity.builder()
            .agentId(history.getAgentId())
            .userId(history.getUserId())
            .gameId(history.getGameId())
            .sessionId(history.getSessionId())
            .roundId(history.getRoundId())
            .parentRoundId(history.getParentRoundId())
            .thisMode(history.getThisMode())
            .nextMode(history.getNextMode())
            .betAmount(history.getBetAmount())
            .totalWin(history.getTotalWin())
            .trialMode(history.isTrialMode())
            .screen(history.getScreen())
            .wins(mapWins(history.getWins()))
            .freeSpinsTotal(history.getFreeSpinsTotal())
            .freeSpinsRemain(history.getFreeSpinsRemain())
            .respinsRemain(history.getRespinsRemain())
            .jackpotWonTier(history.getJackpotWonTier())
            .jackpotWonAmount(history.getJackpotWonAmount())
            .jackpotContribution(history.getJackpotContribution())
            .timestamp(history.getTimestamp())
            .build();

    repository.save(entity);
  }

  private List<SlotHistoryEntity.WinLineRecord> mapWins(List<WinDetail> wins) {
    if (wins == null || wins.isEmpty()) {
      return Collections.emptyList();
    }
    return wins.stream()
        .map(
            w ->
                SlotHistoryEntity.WinLineRecord.builder()
                    .payline(w.getLineId())
                    .symbol(w.getSymbolId())
                    .occurs(w.getCount())
                    .win(BigDecimal.valueOf(w.getAmount().getAmount()))
                    .type(w.getType())
                    .build())
        .collect(Collectors.toList());
  }
}
