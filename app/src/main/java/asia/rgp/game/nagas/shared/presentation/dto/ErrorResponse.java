package asia.rgp.game.nagas.shared.presentation.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
  private String code;
  private String message;
  private LocalDateTime timestamp;
}
