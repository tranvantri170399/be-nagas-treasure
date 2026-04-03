package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.adapter;

import asia.rgp.game.nagas.modules.slot.application.port.out.SlotHistoryPort;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotHistory;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.SlotHistoryEntity;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository.MongoSlotHistoryRepository;
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
            .roundId(history.getRoundId())
            .parentRoundId(history.getParentRoundId())
            .round(history.getRound())
            .subRound(history.getSubRound())
            .userId(history.getUserId())
            .gameId(history.getGameId())
            .sessionId(history.getSessionId())
            .totalBet(history.getTotalBet())
            .displayBet(history.getDisplayBet())
            .totalWin(history.getTotalWin())
            .balanceAfter(history.getBalanceAfter())
            .screenData(history.getScreen())
            .winData(history.getWins())
            .type(history.getType())
            .createdAt(history.getCreatedAt())
            .build();

    repository.save(entity);
  }
}
