package asia.rgp.game.nagas.modules.slot.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

/** [APPLICATION DTO] Response data returned to the presentation layer. */
@Getter
@Builder
public class SpinResultResponse {
  private final int[][] matrix;
  private final long winAmount;
  private final String transactionId;
}
