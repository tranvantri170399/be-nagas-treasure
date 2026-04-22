package asia.rgp.game.nagas.infrastructure.grpc.handler;

import asia.rgp.game.nagas.infrastructure.grpc.MessagePackHelper;
import asia.rgp.game.nagas.infrastructure.grpc.PluginCommand;
import asia.rgp.game.nagas.modules.slot.application.port.in.SpinUseCase;
import asia.rgp.game.nagas.modules.slot.presentation.dto.response.SlotResultResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles JOIN / CONNECT command (cmd = 1005).
 *
 * <p>Responsibilities (handler layer only):
 *
 * <ul>
 *   <li>Extract connection params from decoded payload
 *   <li>Delegate to {@link SpinUseCase#getInitialState} (no business logic here)
 *   <li>Encode result as MessagePack response bytes
 * </ul>
 *
 * <p>Expected payload keys:
 *
 * <pre>
 * {
 *   "cmd":      1005,
 *   "agent_id": "agent-1",
 *   "user_id":  "user-42",
 *   "game_id":  "nagas_treasure"
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectHandler {

  private final SpinUseCase spinUseCase;
  private final ObjectMapper objectMapper;

  /**
   * Process JOIN command.
   *
   * @param connectionId WsProxy connection ID
   * @param sessionId Player session ID (also used as ZMQ topic key)
   * @param zoneId Zone context from gRPC request
   * @param payload Decoded MessagePack map from request data
   * @return MessagePack-encoded response bytes [5, {cmd, c:0, ...result}]
   */
  @SuppressWarnings("unchecked")
  public byte[] handle(
      String connectionId, String sessionId, String zoneId, Map<String, Object> payload)
      throws Exception {

    String agentId = payloadString(payload, "agency_id", "agencyId", "agent_id", "agentId", "");
    String userId = payloadString(payload, "user_id", "userId", "user_id", "userId", "");
    String gameId =
        payloadString(payload, "game_id", "gameId", "game_id", "gameId", "nagas_treasure");

    log.info(
        "[ConnectHandler] JOIN | agent={} user={} game={} session={} zone={}",
        agentId,
        userId,
        gameId,
        sessionId,
        zoneId);

    SlotResultResponse result = spinUseCase.getInitialState(agentId, userId, gameId, sessionId);

    Map<String, Object> resultMap = objectMapper.convertValue(result, Map.class);
    return MessagePackHelper.encodeResponse(PluginCommand.JOIN.getCode(), resultMap);
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
}
