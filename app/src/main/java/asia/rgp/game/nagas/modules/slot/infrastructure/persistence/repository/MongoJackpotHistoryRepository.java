package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository;

import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.JackpotHistoryEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoJackpotHistoryRepository
    extends MongoRepository<JackpotHistoryEntity, String> {

  List<JackpotHistoryEntity> findByAgentIdAndJackpotTypeInOrderByCreatedAtDesc(
      String agentId, List<String> jackpotTypes, Pageable pageable);
}
