package asia.rgp.game.nagas.modules.slot.domain.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JackpotHistory {
  private final String id;
  private final String winId;
  private final String agencyId;
  private final String userId;
  private final String username;
  private final String sessionId;
  private final String jackpotType;
  private final double amount;
  private final Instant createdAt;
}
