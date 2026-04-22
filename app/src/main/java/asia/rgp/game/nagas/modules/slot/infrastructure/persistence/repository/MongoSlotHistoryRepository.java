package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository;

import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.SlotHistoryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoSlotHistoryRepository extends MongoRepository<SlotHistoryEntity, String> {
  List<SlotHistoryEntity> findByAgentIdAndUserIdOrderByTimestampDesc(String agentId, String userId);

  List<SlotHistoryEntity> findByAgentIdAndUserIdOrderByTimestampDesc(
      String agentId, String userId, Pageable pageable);

  List<SlotHistoryEntity> findByAgentIdAndUserIdAndGameIdOrderByTimestampDesc(
      String agentId, String userId, String gameId, Pageable pageable);

  Optional<SlotHistoryEntity> findByAgentIdAndRoundId(String agentId, String roundId);
}
