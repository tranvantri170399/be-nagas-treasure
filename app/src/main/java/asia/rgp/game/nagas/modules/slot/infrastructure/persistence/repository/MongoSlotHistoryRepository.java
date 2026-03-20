package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository;

import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.SlotHistoryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MongoSlotHistoryRepository extends MongoRepository<SlotHistoryEntity, String> {
    List<SlotHistoryEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}