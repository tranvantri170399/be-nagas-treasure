package asia.rgp.game.nagas.modules.slot.application.port.out;

import asia.rgp.game.nagas.modules.slot.domain.model.SlotHistory;
import java.util.List;
import java.util.Optional;

public interface SlotHistoryPort {
  void save(SlotHistory history);

  List<SlotHistory> findByUser(String agentId, String userId, String gameId, int limit, int offset);

  Optional<SlotHistory> findByRoundId(String agentId, String roundId);
}
