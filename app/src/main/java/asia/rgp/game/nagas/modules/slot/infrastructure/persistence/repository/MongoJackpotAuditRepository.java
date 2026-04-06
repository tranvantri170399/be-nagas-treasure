package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository;

import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.JackpotAuditEntity;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoJackpotAuditRepository extends MongoRepository<JackpotAuditEntity, String> {
  Optional<JackpotAuditEntity> findByWinId(String winId);
}
