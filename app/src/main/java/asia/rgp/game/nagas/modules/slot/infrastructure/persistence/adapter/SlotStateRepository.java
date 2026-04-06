package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.adapter;

import asia.rgp.game.nagas.infrastructure.cache.HotCacheService;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotState;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SlotStateRepository {

  private final HotCacheService hotCache;
  private static final String STATE_KEY_PREFIX = "slot_state:";

  public void save(SlotState state) {
    String key =
        STATE_KEY_PREFIX + state.getAgentId() + ":" + state.getUserId() + ":" + state.getGameId();
    hotCache.put(key, state, Duration.ofHours(24));
  }

  public Optional<SlotState> find(String agentId, String userId, String gameId) {
    String key = STATE_KEY_PREFIX + agentId + ":" + userId + ":" + gameId;
    return hotCache.get(key, SlotState.class);
  }

  public void delete(String agentId, String userId, String gameId) {
    String key = STATE_KEY_PREFIX + agentId + ":" + userId + ":" + gameId;
    hotCache.evict(key);
  }
}
