package asia.rgp.game.nagas.infrastructure.grpc.handler;

import asia.rgp.game.nagas.infrastructure.grpc.MessagePackHelper;
import asia.rgp.game.nagas.infrastructure.grpc.PluginCommand;
import asia.rgp.game.nagas.modules.slot.application.dto.request.BuyFeatureCommand;
import asia.rgp.game.nagas.modules.slot.application.dto.request.SpinCommand;
import asia.rgp.game.nagas.modules.slot.application.port.in.SpinUseCase;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.modules.slot.presentation.dto.response.SlotResultResponse;
import asia.rgp.game.nagas.shared.domain.model.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles SPIN, BUY_FEATURE, and LAST_SESSION commands.
 *
 * <p>Handler responsibilities only:
 *
 * <ul>
 *   <li>Parse request payload
 *   <li>Map to command objects
 *   <li>Call use case
 *   <li>Encode and return response bytes
 * </ul>
 *
 * <p>No business logic lives here. All spin/feature logic is in {@link SpinUseCase}.
 *
 * <p>Expected payload for SPIN (cmd = 1500):
 *
 * <pre>
 * {
 *   "cmd":        1500,
 *   "agent_id":   "agent-1",
 *   "user_id":    "user-42",
 *   "game_id":    "nagas_treasure",
 *   "bet_amount": 100,
 *   "trial_mode": false
 * }
 * </pre>
 *
 * <p>Expected payload for BUY_FEATURE (cmd = 1501):
 *
 * <pre>
 * {
 *   "cmd":        1501,
 *   "agent_id":   "agent-1",
 *   "user_id":    "user-42",
 *   "game_id":    "nagas_treasure",
 *   "bet_amount": 100,
 *   "feature":    "freeSpins" | "holdAndWin"
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpinHandler {

  private final SpinUseCase spinUseCase;
  private final ObjectMapper objectMapper;

  /** Handle SPIN command (cmd = 1500). */
  @SuppressWarnings("unchecked")
  public byte[] handleSpin(String sessionId, Map<String, Object> payload) throws Exception {
    String agentId = (String) payload.getOrDefault("agent_id", "");
    String userId = (String) payload.getOrDefault("user_id", "");
    String gameId = (String) payload.getOrDefault("game_id", "nagas_treasure");
    long betAmount = ((Number) payload.getOrDefault("bet_amount", 100L)).longValue();
    boolean trial = Boolean.TRUE.equals(payload.get("trial_mode"));

    log.info(
        "[SpinHandler] SPIN | agent={} user={} bet={} trial={}", agentId, userId, betAmount, trial);

    SpinCommand command =
        SpinCommand.builder()
            .agentId(agentId)
            .userId(userId)
            .gameId(gameId)
            .sessionId(sessionId)
            .betAmount(Money.of(betAmount))
            .trialMode(trial)
            .build();

    SlotResultResponse result = spinUseCase.execute(command);
    Map<String, Object> resultMap = objectMapper.convertValue(result, Map.class);
    return MessagePackHelper.encodeResponse(PluginCommand.SPIN.getCode(), resultMap);
  }

  /**
   * Handle BUY_FEATURE command (cmd = 1501). Routes to free spins or hold-and-win based on {@code
   * feature} field in payload.
   */
  @SuppressWarnings("unchecked")
  public byte[] handleBuyFeature(String sessionId, Map<String, Object> payload) throws Exception {
    String agentId = (String) payload.getOrDefault("agent_id", "");
    String userId = (String) payload.getOrDefault("user_id", "");
    String gameId = (String) payload.getOrDefault("game_id", "nagas_treasure");
    long betAmount = ((Number) payload.getOrDefault("bet_amount", 100L)).longValue();
    String feature = (String) payload.getOrDefault("feature", SlotConstants.FEATURE_FREE_SPINS);
    boolean trial = Boolean.TRUE.equals(payload.get("trial_mode"));

    log.info(
        "[SpinHandler] BUY_FEATURE | agent={} user={} feature={} bet={}",
        agentId,
        userId,
        feature,
        betAmount);

    BuyFeatureCommand command =
        BuyFeatureCommand.builder()
            .agentId(agentId)
            .userId(userId)
            .gameId(gameId)
            .sessionId(sessionId)
            .featureName(feature)
            .betAmount(Money.of(betAmount))
            .trialMode(trial)
            .build();

    SlotResultResponse result =
        SlotConstants.FEATURE_HOLD_AND_WIN.equalsIgnoreCase(feature)
            ? spinUseCase.executeBuyHoldAndWin(command)
            : spinUseCase.executeBuyFeature(command);

    Map<String, Object> resultMap = objectMapper.convertValue(result, Map.class);
    return MessagePackHelper.encodeResponse(PluginCommand.BUY_FEATURE.getCode(), resultMap);
  }

  /** Handle LAST_SESSION command (cmd = 1502) — returns current state or initial state. */
  @SuppressWarnings("unchecked")
  public byte[] handleLastSession(String agentId, String userId, String gameId, String sessionId)
      throws Exception {

    log.info("[SpinHandler] LAST_SESSION | agent={} user={} game={}", agentId, userId, gameId);

    SlotResultResponse result = spinUseCase.getInitialState(agentId, userId, gameId, sessionId);
    Map<String, Object> resultMap = objectMapper.convertValue(result, Map.class);
    return MessagePackHelper.encodeResponse(PluginCommand.LAST_SESSION.getCode(), resultMap);
  }
}
