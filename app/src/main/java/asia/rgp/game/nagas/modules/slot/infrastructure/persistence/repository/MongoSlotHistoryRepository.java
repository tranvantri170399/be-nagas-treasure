package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository;

import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.SlotHistoryEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoSlotHistoryRepository extends MongoRepository<SlotHistoryEntity, String> {
  List<SlotHistoryEntity> findByAgentIdAndUserIdOrderByTimestampDesc(String agentId, String userId);
}
