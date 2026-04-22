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
    String agentId = payloadString(payload, "agency_id", "agencyId", "agent_id", "agentId", "");
    String userId = payloadString(payload, "user_id", "userId", "user_id", "userId", "");
    String gameId =
        payloadString(payload, "game_id", "gameId", "game_id", "gameId", "nagas_treasure");
    long betAmount = payloadLong(payload, "bet_amount", "betAmount", 100L);
    boolean trial = payloadBoolean(payload, "trial_mode", "trialMode", false);

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
    String agentId = payloadString(payload, "agency_id", "agencyId", "agent_id", "agentId", "");
    String userId = payloadString(payload, "user_id", "userId", "user_id", "userId", "");
    String gameId =
        payloadString(payload, "game_id", "gameId", "game_id", "gameId", "nagas_treasure");
    long betAmount = payloadLong(payload, "bet_amount", "betAmount", 100L);
    String feature = (String) payload.getOrDefault("feature", SlotConstants.FEATURE_FREE_SPINS);
    boolean trial = payloadBoolean(payload, "trial_mode", "trialMode", false);

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

  private String payloadString(
      Map<String, Object> payload,
      String primarySnakeKey,
      String primaryCamelKey,
      String secondarySnakeKey,
      String secondaryCamelKey,
      String fallback) {
    Object value = payload.get(primarySnakeKey);
    if (value == null || String.valueOf(value).isBlank()) {
      value = payload.get(primaryCamelKey);
    }
    if (value == null || String.valueOf(value).isBlank()) {
      value = payload.get(secondarySnakeKey);
    }
    if (value == null || String.valueOf(value).isBlank()) {
      value = payload.get(secondaryCamelKey);
    }
    if (value == null) {
      return fallback;
    }
    String result = String.valueOf(value);
    return result.isBlank() ? fallback : result;
  }

  private long payloadLong(
      Map<String, Object> payload, String snakeKey, String camelKey, long fallback) {
    Object value = payload.get(snakeKey);
    if (value == null) {
      value = payload.get(camelKey);
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String string) {
      try {
        return Long.parseLong(string);
      } catch (NumberFormatException ignored) {
        return fallback;
      }
    }
    return fallback;
  }

  private boolean payloadBoolean(
      Map<String, Object> payload, String snakeKey, String camelKey, boolean fallback) {
    Object value = payload.get(snakeKey);
    if (value == null) {
      value = payload.get(camelKey);
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof String string) {
      return Boolean.parseBoolean(string);
    }
    return fallback;
  }
}
