package asia.rgp.game.nagas.modules.slot.application.port.out;

import asia.rgp.game.nagas.modules.slot.domain.model.JackpotHistory;
import java.util.List;

public interface JackpotHistoryPort {
  void save(JackpotHistory history);

  List<JackpotHistory> findByAgentId(String agentId, int limit);
}
