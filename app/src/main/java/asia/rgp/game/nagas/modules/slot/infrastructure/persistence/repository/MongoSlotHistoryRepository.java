package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository;

import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.SlotHistoryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoSlotHistoryRepository extends MongoRepository<SlotHistoryEntity, String> {
  List<SlotHistoryEntity> findByAgencyIdAndUserIdOrderByTimestampDesc(
      String agencyId, String userId);

  List<SlotHistoryEntity> findByAgencyIdAndUserIdOrderByTimestampDesc(
      String agencyId, String userId, Pageable pageable);

  List<SlotHistoryEntity> findByAgencyIdAndUserIdAndGameIdOrderByTimestampDesc(
      String agencyId, String userId, String gameId, Pageable pageable);

  Optional<SlotHistoryEntity> findByAgencyIdAndRoundId(String agencyId, String roundId);
}
