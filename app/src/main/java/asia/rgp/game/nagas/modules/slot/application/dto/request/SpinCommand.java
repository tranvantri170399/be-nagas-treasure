package asia.rgp.game.nagas.modules.slot.application.dto.request;

import asia.rgp.game.nagas.shared.domain.model.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpinCommand {
  private String userId;
  private String gameId;
  private Money betAmount;
  private String sessionId;
}
