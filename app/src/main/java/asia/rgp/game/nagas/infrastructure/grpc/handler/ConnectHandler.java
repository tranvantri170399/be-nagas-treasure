package asia.rgp.game.nagas.infrastructure.grpc.handler;

import asia.rgp.game.nagas.infrastructure.grpc.MessagePackHelper;
import asia.rgp.game.nagas.infrastructure.grpc.PluginCommand;
import asia.rgp.game.nagas.modules.slot.application.port.in.SpinUseCase;
import asia.rgp.game.nagas.modules.slot.presentation.dto.response.SlotResultResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
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

    String agencyId = payloadString(payload, "agency_id", "agencyId", "agent_id", "agentId", "");
    String userId = payloadString(payload, "user_id", "userId", "user_id", "userId", "");
    String gameId =
        payloadString(payload, "game_id", "gameId", "game_id", "gameId", "nagas_treasure");

    log.info(
        "[ConnectHandler] JOIN | agency={} user={} game={} session={} zone={}",
        agencyId,
        userId,
        gameId,
        sessionId,
        zoneId);

    SlotResultResponse result = spinUseCase.getInitialState(agencyId, userId, gameId, sessionId);
    log.info(
        "[ConnectHandler] JOIN done | agency={} user={} game={} session={} balance={}",
        agencyId,
        userId,
        gameId,
        sessionId,
        result.getData() != null && result.getData().getControl() != null
            ? result.getData().getControl().getBalance()
            : null);

    Map<String, Object> resultMap = objectMapper.convertValue(result, Map.class);
    return MessagePackHelper.encodeResponse(
        PluginCommand.JOIN.getCode(), buildJoinPayload(resultMap, sessionId));
  }

  private Map<String, Object> buildJoinPayload(Map<String, Object> resultMap, String sessionId) {
    Map<String, Object> payload = new LinkedHashMap<>();
    Map<String, Object> dataMap = nestedMap(resultMap.get("data"));
    Map<String, Object> controlMap = nestedMap(dataMap.get("control"));
    Map<String, Object> roundMap = nestedMap(dataMap.get("round"));
    Map<String, Object> transactionIdMap = nestedMap(roundMap.get("transactionId"));
    Map<String, Object> parentIdMap = nestedMap(roundMap.get("parentId"));
    Map<String, Object> resultDetailsMap = nestedMap(roundMap.get("result"));
    Map<String, Object> featuresMap = nestedMap(resultDetailsMap.get("features"));
    Map<String, Object> jackpotPoolsMap = nestedMap(featuresMap.get("jackpotPools"));

    if (!transactionIdMap.isEmpty() && isBlank(transactionIdMap.get("sessionId"))) {
      transactionIdMap.put("sessionId", sessionId);
      roundMap.put("transactionId", transactionIdMap);
    }
    if (!parentIdMap.isEmpty() && isBlank(parentIdMap.get("sessionId"))) {
      parentIdMap.put("sessionId", sessionId);
      roundMap.put("parentId", parentIdMap);
    }
    if (!resultDetailsMap.isEmpty() && isBlank(resultDetailsMap.get("sessionId"))) {
      resultDetailsMap.put("sessionId", sessionId);
      roundMap.put("result", resultDetailsMap);
    }
    if (!roundMap.isEmpty()) {
      dataMap.put("round", roundMap);
    }

    putIfPresent(payload, "type", resultMap.get("type"));
    putIfPresent(payload, "data", dataMap.isEmpty() ? resultMap.get("data") : dataMap);
    payload.put("sessionId", sessionId);
    putIfPresent(payload, "balance", controlMap.get("balance"));
    putIfPresent(
        payload,
        "currency",
        firstNonBlank(roundMap.get("currency"), resultDetailsMap.get("currency")));
    putIfPresent(payload, "roundId", roundMap.get("roundId"));
    putIfPresent(payload, "parentRoundId", roundMap.get("parentRoundId"));
    putIfPresent(payload, "transactionId", transactionIdMap.isEmpty() ? null : transactionIdMap);
    putIfPresent(payload, "parentId", parentIdMap.isEmpty() ? null : parentIdMap);
    putIfPresent(
        payload,
        "thisMode",
        firstNonBlank(resultDetailsMap.get("thisMode"), roundMap.get("thisMode")));
    putIfPresent(
        payload,
        "nextMode",
        firstNonBlank(resultDetailsMap.get("nextMode"), roundMap.get("nextMode")));
    putIfPresent(
        payload,
        "totalBet",
        firstNonBlank(roundMap.get("totalBet"), resultDetailsMap.get("totalBet")));
    putIfPresent(
        payload,
        "totalWin",
        firstNonBlank(roundMap.get("totalWin"), resultDetailsMap.get("totalWin")));
    putIfPresent(payload, "createdAt", roundMap.get("createdAt"));
    putIfPresent(payload, "endsSuperround", roundMap.get("endsSuperround"));
    putIfPresent(payload, "features", featuresMap.isEmpty() ? null : featuresMap);
    putIfPresent(payload, "jackpotPools", jackpotPoolsMap.isEmpty() ? null : jackpotPoolsMap);
    Object stages = resultDetailsMap.get("stages");
    putIfPresent(payload, "stages", stages);
    putIfPresent(payload, "screen", firstMapFromList(stages).get("screen"));
    return payload;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> nestedMap(Object value) {
    if (value instanceof Map<?, ?>) {
      return (Map<String, Object>) value;
    }
    return new LinkedHashMap<>();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> firstMapFromList(Object value) {
    if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
      return (Map<String, Object>) list.get(0);
    }
    return new LinkedHashMap<>();
  }

  private Object firstNonBlank(Object primary, Object secondary) {
    if (!isBlank(primary)) {
      return primary;
    }
    return isBlank(secondary) ? null : secondary;
  }

  private void putIfPresent(Map<String, Object> target, String key, Object value) {
    if (value == null) {
      return;
    }
    if (value instanceof String string && string.isBlank()) {
      return;
    }
    target.put(key, value);
  }

  private boolean isBlank(Object value) {
    return value == null || (value instanceof String string && string.isBlank());
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
