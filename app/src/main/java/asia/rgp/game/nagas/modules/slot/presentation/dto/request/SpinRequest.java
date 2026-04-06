package asia.rgp.game.nagas.modules.slot.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpinRequest(
    @NotBlank(message = "agent_id is required") @JsonProperty("agent_id") String agentId,
    @NotNull(message = "user_id is not null") @JsonProperty("user_id") String userId,
    @NotBlank(message = "game_id is not null") @JsonProperty("game_id") String gameId,
    @NotNull(message = "bet_amount is not null")
        @Min(value = 1, message = "bet_amount > 0")
        @JsonProperty("bet_amount")
        Long betAmount,
    @NotBlank(message = "session_id is not null") @JsonProperty("session_id") String sessionId,
    @JsonProperty("trial_mode") Boolean trialMode) {

  public boolean isTrialMode() {
    return Boolean.TRUE.equals(trialMode);
  }
}
