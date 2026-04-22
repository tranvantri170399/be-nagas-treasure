package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.adapter;

import asia.rgp.game.nagas.modules.slot.application.port.out.SlotHistoryPort;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotHistory;
import asia.rgp.game.nagas.modules.slot.domain.model.WinDetail;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.SlotHistoryEntity;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository.MongoSlotHistoryRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

  @Override
  public List<SlotHistory> findByUser(
      String agentId, String userId, String gameId, int limit, int offset) {
    int page = Math.max(offset, 0) / Math.max(limit, 1);
    int size = Math.max(limit, 1);
    List<SlotHistoryEntity> rows =
        (gameId == null || gameId.isBlank())
            ? repository.findByAgentIdAndUserIdOrderByTimestampDesc(
                agentId, userId, PageRequest.of(page, size))
            : repository.findByAgentIdAndUserIdAndGameIdOrderByTimestampDesc(
                agentId, userId, gameId, PageRequest.of(page, size));
    return rows.stream().map(this::toDomain).collect(Collectors.toList());
  }

  @Override
  public Optional<SlotHistory> findByRoundId(String agentId, String roundId) {
    return repository.findByAgentIdAndRoundId(agentId, roundId).map(this::toDomain);
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

  private SlotHistory toDomain(SlotHistoryEntity entity) {
    return SlotHistory.builder()
        .roundId(entity.getRoundId())
        .parentRoundId(entity.getParentRoundId())
        .agentId(entity.getAgentId())
        .userId(entity.getUserId())
        .gameId(entity.getGameId())
        .sessionId(entity.getSessionId())
        .thisMode(entity.getThisMode())
        .nextMode(entity.getNextMode())
        .betAmount(entity.getBetAmount())
        .totalWin(entity.getTotalWin())
        .trialMode(entity.isTrialMode())
        .screen(entity.getScreen())
        .wins(
            entity.getWins() == null
                ? Collections.emptyList()
                : entity.getWins().stream()
                    .map(
                        w ->
                            WinDetail.builder()
                                .lineId(w.getPayline())
                                .symbolId(w.getSymbol())
                                .count(w.getOccurs())
                                .amount(
                                    asia.rgp.game.nagas.shared.domain.model.Money.of(
                                        w.getWin().doubleValue()))
                                .type(w.getType())
                                .build())
                    .collect(Collectors.toList()))
        .freeSpinsTotal(entity.getFreeSpinsTotal())
        .freeSpinsRemain(entity.getFreeSpinsRemain())
        .respinsRemain(entity.getRespinsRemain())
        .jackpotWonTier(entity.getJackpotWonTier())
        .jackpotWonAmount(entity.getJackpotWonAmount())
        .jackpotContribution(entity.getJackpotContribution())
        .timestamp(entity.getTimestamp())
        .build();
  }
}
