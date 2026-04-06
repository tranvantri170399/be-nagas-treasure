package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.adapter;

import asia.rgp.game.nagas.modules.slot.application.port.out.JackpotHistoryPort;
import asia.rgp.game.nagas.modules.slot.domain.model.JackpotHistory;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.JackpotHistoryEntity;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository.MongoJackpotHistoryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JackpotHistoryPersistenceAdapter implements JackpotHistoryPort {

  private final MongoJackpotHistoryRepository repository;

  @Override
  public void save(JackpotHistory history) {
    JackpotHistoryEntity entity =
        JackpotHistoryEntity.builder()
            .id(UUID.randomUUID().toString())
            .winId(history.getWinId())
            .agentId(history.getAgentId())
            .userId(history.getUserId())
            .username(history.getUsername())
            .sessionId(history.getSessionId())
            .jackpotType(history.getJackpotType())
            .amount(history.getAmount())
            .createdAt(history.getCreatedAt())
            .build();
    repository.save(entity);
  }

  @Override
  public List<JackpotHistory> findByAgentId(String agentId, int limit) {
    // GDD 11.3: Only show GRAND and MAJOR jackpot history
    List<String> visibleTypes = List.of(SlotConstants.JACKPOT_DIAMOND, SlotConstants.JACKPOT_RUBY);

    return repository
        .findByAgentIdAndJackpotTypeInOrderByCreatedAtDesc(
            agentId, visibleTypes, PageRequest.of(0, limit))
        .stream()
        .map(
            e ->
                JackpotHistory.builder()
                    .id(e.getId())
                    .winId(e.getWinId())
                    .agentId(e.getAgentId())
                    .userId(e.getUserId())
                    .username(e.getUsername())
                    .sessionId(e.getSessionId())
                    .jackpotType(e.getJackpotType())
                    .amount(e.getAmount())
                    .createdAt(e.getCreatedAt())
                    .build())
        .toList();
  }
}
