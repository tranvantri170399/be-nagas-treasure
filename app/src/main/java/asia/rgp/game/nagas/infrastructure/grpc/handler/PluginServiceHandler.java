package asia.rgp.game.nagas.infrastructure.grpc.handler;

import asia.rgp.game.nagas.infrastructure.grpc.MessagePackHelper;
import asia.rgp.game.nagas.infrastructure.grpc.PluginCommand;
import asia.rgp.game.nagas.infrastructure.grpc.PluginSessionStore;
import asia.rgp.game.nagas.infrastructure.grpc.PluginSessionStore.SessionAuth;
import asia.rgp.game.nagas.infrastructure.grpc.WalletRequestContext;
import asia.rgp.game.nagas.infrastructure.grpc.ZmqTopicHelper;
import asia.rgp.game.nagas.infrastructure.grpc.generated.BatchPluginRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.BatchPluginResponse;
import asia.rgp.game.nagas.infrastructure.grpc.generated.ConnectAndCallRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.DisconnectBatchRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.DisconnectRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.FetchConnectCommandsRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.FetchConnectCommandsResponse;
import asia.rgp.game.nagas.infrastructure.grpc.generated.InteropRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.InteropResponse;
import asia.rgp.game.nagas.infrastructure.grpc.generated.PluginInfo;
import asia.rgp.game.nagas.infrastructure.grpc.generated.PluginRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.PluginResponse;
import asia.rgp.game.nagas.infrastructure.grpc.generated.PluginServiceGrpc;
import asia.rgp.game.nagas.infrastructure.grpc.generated.SyncSession;
import asia.rgp.game.nagas.infrastructure.grpc.generated.SyncSessionsRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.SyncSessionsResponse;
import asia.rgp.game.nagas.infrastructure.grpc.generated.ZmqMetadata;
import asia.rgp.game.nagas.infrastructure.grpc.generated.ZmqResponse;
import asia.rgp.game.nagas.infrastructure.zmq.ZmqPublisherPort;
import asia.rgp.game.nagas.modules.slot.application.port.out.SlotHistoryPort;
import asia.rgp.game.nagas.modules.slot.application.port.out.WalletPort;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotHistory;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Main gRPC service implementation — acts as the transport entry point.
 *
 * <p>This class is a {@link BindableService} and is automatically picked up by {@link
 * asia.rgp.game.nagas.infrastructure.config.grpc.StandaloneGrpcServerConfig}.
 *
 * <p>Service contract matches be-wsproxy's plugin.proto exactly:
 *
 * <ul>
 *   <li>Wire path: /PluginService/ConnectAndCall, /PluginService/Call, /PluginService/Disconnect
 *   <li>ConnectAndCall: initial join + first command (user info in ConnectAndCallRequest.user)
 *   <li>Call: subsequent commands (sessionId is in PluginRequest.username per be-wsproxy
 *       convention)
 *   <li>Disconnect: session cleanup
 * </ul>
 *
 * <p>Data flow:
 *
 * <pre>
 *   WsProxy ──gRPC──▶ PluginServiceHandler
 *                          │
 *                    decode cmd
 *                          │
 *              ┌───────────┼──────────────┐
 *              ▼           ▼              ▼
 *       ConnectHandler  SpinHandler  (future handlers)
 *              │           │
 *              └─────┬─────┘
 *                    ▼
 *              ZMQ publish result ──▶ WsProxy ──▶ Player (WebSocket)
 *                    │
 *              gRPC ZmqResponse (with topic metadata)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginServiceHandler extends PluginServiceGrpc.PluginServiceImplBase {

  private final SpinHandler spinHandler;
  private final ConnectHandler connectHandler;
  private final ZmqPublisherPort zmqPublisher;
  private final WalletPort walletPort;
  private final SlotHistoryPort slotHistoryPort;
  private final PluginSessionStore sessionStore;

  // ─────────────────────────────────────────────
  // gRPC Methods
  // ─────────────────────────────────────────────

  @Override
  public void connectAndCall(ConnectAndCallRequest request, StreamObserver<ZmqResponse> observer) {
    String zone = request.getZone();
    String sessionId = request.getUser().getSessionId();
    String userId = request.getUser().getId();
    String username = request.getUser().getUsername();
    log.info(
        "[gRPC] ConnectAndCall | pluginName={} zone={} session={} userId={} username={} dataBytes={} userParamBytes={}",
        request.getPluginName(),
        zone,
        sessionId,
        userId,
        username,
        request.getData().size(),
        request.getUser().getParameters().size());
    if (userId == null || userId.isBlank()) {
      log.warn(
          "[gRPC] ConnectAndCall | PluginUser.id is empty for session={}, downstream operations may fail",
          sessionId);
    }
    byte[] rawData = request.getData().toByteArray();

    try {
      Map<String, Object> payload = MessagePackHelper.decode(rawData);
      Map<String, Object> userParams =
          MessagePackHelper.decode(request.getUser().getParameters().toByteArray());
      log.info(
          "[gRPC] ConnectAndCall | decoded payloadKeys={} userParamKeys={}",
          payload.keySet(),
          userParams.keySet());
      String token = extractToken(userParams);
      if (token == null) {
        log.warn(
            "[gRPC] Missing token in user.parameters. keys={} session={}",
            userParams.keySet(),
            sessionId);
        throw new IllegalArgumentException("MISSING_AUTH_TOKEN");
      }
      String agency = extractAgency(userParams);
      if (agency == null) {
        agency = payloadString(payload, "agency_id", "agencyId", "agent_id", "agentId", "");
      }

      payload.put("user_id", userId);
      payload.put("agency_id", agency);

      SessionAuth auth =
          SessionAuth.builder()
              .sessionId(sessionId)
              .userId(userId)
              .username(username)
              .agency(agency)
              .token(token)
              .zone(zone)
              .pluginName(request.getPluginName())
              .build();
      sessionStore.put(sessionId, auth);

      log.info(
          "[gRPC] ConnectAndCall | rawDataBytes={} decodedPayload={} authAgency={} authStored=true",
          rawData.length,
          payload,
          agency);
      int cmdCode = resolveCmdCode(payload);
      log.info("[gRPC] ConnectAndCall | resolved cmdCode={} from payload", cmdCode);

      log.info(
          "[gRPC] ConnectAndCall | cmd={} session={} zone={} userId={}",
          cmdCode,
          sessionId,
          zone,
          userId);

      PluginCommand command =
          PluginCommand.fromCode(cmdCode)
              .orElseThrow(() -> new IllegalArgumentException("Unknown cmd: " + cmdCode));
      log.info(
          "[gRPC] ConnectAndCall | routing command={}({}) session={} zone={}",
          command,
          command.getCode(),
          sessionId,
          zone);

      WalletRequestContext.set(
          WalletRequestContext.Context.builder()
              .sessionId(sessionId)
              .userId(userId)
              .agency(agency)
              .token(token)
              .build());
      byte[] responseData;
      try {
        responseData = routeToHandler(command, sessionId, sessionId, zone, payload);
      } finally {
        WalletRequestContext.clear();
      }
      log.info(
          "[gRPC] ConnectAndCall | handler completed command={} responseBytes={} session={}",
          command,
          responseData == null ? 0 : responseData.length,
          sessionId);

      String topic = ZmqTopicHelper.buildTopic(zone, sessionId);
      zmqPublisher.publish(topic, responseData);
      log.info("[gRPC] ConnectAndCall | published response topic={} session={}", topic, sessionId);

      observer.onNext(zmqResponseWithTopic(topic));
      observer.onCompleted();

    } catch (IllegalArgumentException e) {
      log.warn("[gRPC] ConnectAndCall bad request session={}: {}", sessionId, e.getMessage());
      publishErrorAndComplete(rawData, 400, e.getMessage(), zone, sessionId, observer);
    } catch (Exception e) {
      log.error("[gRPC] ConnectAndCall error session={}: {}", sessionId, e.getMessage(), e);
      publishErrorAndComplete(rawData, 500, "Internal server error", zone, sessionId, observer);
    }
  }

  private String asNonBlankString(Object value) {
    if (value == null) {
      return null;
    }
    String str = String.valueOf(value).trim();
    return str.isEmpty() ? null : str;
  }

  private String extractToken(Map<String, Object> userParams) {
    String token = asNonBlankString(userParams.get("token"));
    if (token != null) {
      return token;
    }

    token = asNonBlankString(userParams.get("accessToken"));
    if (token != null) {
      return token;
    }

    token = asNonBlankString(userParams.get("access_token"));
    if (token != null) {
      return token;
    }

    Object nestedUserAgent = userParams.get("userAgent");
    if (nestedUserAgent instanceof Map<?, ?> map) {
      token = asNonBlankString(map.get("token"));
      if (token != null) {
        return token;
      }
      token = asNonBlankString(map.get("accessToken"));
      if (token != null) {
        return token;
      }
      token = asNonBlankString(map.get("access_token"));
      if (token != null) {
        return token;
      }
    }

    return null;
  }

  private String extractAgency(Map<String, Object> userParams) {
    String agency = asNonBlankString(userParams.get("agency"));
    if (agency != null) {
      return agency;
    }

    agency = asNonBlankString(userParams.get("agencyId"));
    if (agency != null) {
      return agency;
    }

    agency = asNonBlankString(userParams.get("agentId"));
    if (agency != null) {
      return agency;
    }

    agency = asNonBlankString(userParams.get("agency_id"));
    if (agency != null) {
      return agency;
    }

    Object nestedUserAgent = userParams.get("userAgent");
    if (nestedUserAgent instanceof Map<?, ?> map) {
      agency = asNonBlankString(map.get("agency_id"));
      if (agency != null) {
        return agency;
      }
      agency = asNonBlankString(map.get("agencyId"));
      if (agency != null) {
        return agency;
      }

      agency = asNonBlankString(map.get("agentId"));
      if (agency != null) {
        return agency;
      }
    }

    return null;
  }

  private String buildInternalTopic(String command, String pluginName) {
    String cmd = command == null || command.isBlank() ? "event" : command;
    String plugin = pluginName == null || pluginName.isBlank() ? "nagas_treasure" : pluginName;
    return "urn:int:c:" + cmd + ":p:" + plugin;
  }

  @Override
  public void call(PluginRequest request, StreamObserver<PluginResponse> observer) {
    String zone = request.getZone();
    String lookupValue = request.getUsername();
    String resolvedSessionId = lookupValue;
    byte[] rawData = request.getData().toByteArray();
    log.info(
        "[gRPC] Call | zone={} lookupValue={} dataBytes={}", zone, lookupValue, rawData.length);

    try {
      Map<String, Object> payload = MessagePackHelper.decode(rawData);
      SessionAuth auth =
          sessionStore
              .getByUsernameOrSessionId(lookupValue)
              .orElseThrow(() -> new IllegalArgumentException("SESSION_NOT_FOUND"));
      resolvedSessionId = auth.getSessionId();
      payload.put("user_id", auth.getUserId());
      payload.put("agency_id", auth.getAgency());
      log.info(
          "[gRPC] Call | decoded payloadKeys={} payload={} authAgency={} authUserId={} authUsername={} resolvedSessionId={}",
          payload.keySet(),
          payload,
          auth.getAgency(),
          auth.getUserId(),
          auth.getUsername(),
          resolvedSessionId);
      int cmdCode = resolveCmdCode(payload);

      log.info("[gRPC] Call | cmd={} session={} zone={}", cmdCode, resolvedSessionId, zone);

      PluginCommand command =
          PluginCommand.fromCode(cmdCode)
              .orElseThrow(() -> new IllegalArgumentException("Unknown cmd: " + cmdCode));

      WalletRequestContext.set(
          WalletRequestContext.Context.builder()
              .sessionId(resolvedSessionId)
              .userId(auth.getUserId())
              .agency(auth.getAgency())
              .token(auth.getToken())
              .build());
      byte[] responseData;
      try {
        responseData = routeToHandler(command, resolvedSessionId, resolvedSessionId, zone, payload);
      } finally {
        WalletRequestContext.clear();
      }
      log.info(
          "[gRPC] Call | handler completed command={} responseBytes={} session={}",
          command,
          responseData == null ? 0 : responseData.length,
          resolvedSessionId);

      String topic = ZmqTopicHelper.buildTopic(zone, resolvedSessionId);
      zmqPublisher.publish(topic, responseData);
      log.info("[gRPC] Call | published response topic={} session={}", topic, resolvedSessionId);

      observer.onNext(
          PluginResponse.newBuilder().setResult(ByteString.copyFrom(responseData)).build());
      observer.onCompleted();

    } catch (IllegalArgumentException e) {
      log.warn("[gRPC] Call bad request session={}: {}", resolvedSessionId, e.getMessage());
      sendCallError(rawData, 400, e.getMessage(), observer);
    } catch (Exception e) {
      log.error("[gRPC] Call error session={}: {}", resolvedSessionId, e.getMessage(), e);
      sendCallError(rawData, 500, "Internal server error", observer);
    }
  }

  @Override
  public void disconnect(DisconnectRequest request, StreamObserver<ZmqResponse> observer) {
    log.info("[gRPC] Disconnect | session={} zone={}", request.getSessionId(), request.getZone());
    String sessionId = request.getSessionId();
    String zone = request.getZone();
    SessionAuth removed = sessionStore.remove(sessionId).orElse(null);

    if (removed != null) {
      try {
        Map<String, Object> event =
            Map.of(
                "event",
                "disconnect",
                "sessionId",
                sessionId,
                "userId",
                removed.getUserId(),
                "agency",
                removed.getAgency());
        String internalTopic = buildInternalTopic("disconnect", removed.getPluginName());
        zmqPublisher.publish(internalTopic, MessagePackHelper.encodeResponse(0, event));
      } catch (Exception ex) {
        log.warn(
            "[gRPC] Failed to publish internal disconnect event for session={}", sessionId, ex);
      }
    }

    observer.onNext(zmqResponseWithTopic(ZmqTopicHelper.buildTopic(zone, sessionId)));
    observer.onCompleted();
  }

  @Override
  public void disconnectBatch(
      DisconnectBatchRequest request, StreamObserver<ZmqResponse> observer) {
    for (DisconnectRequest item : request.getRequestsList()) {
      sessionStore.remove(item.getSessionId());
    }
    observer.onNext(ZmqResponse.newBuilder().build());
    observer.onCompleted();
  }

  @Override
  public void findSessionsToRemove(
      SyncSessionsRequest request, StreamObserver<SyncSessionsResponse> observer) {
    List<SyncSession> toRemove = new ArrayList<>();
    for (SyncSession session : request.getSessionsList()) {
      if (sessionStore.get(session.getSessionId()).isEmpty()) {
        toRemove.add(session);
      }
    }
    observer.onNext(SyncSessionsResponse.newBuilder().addAllSessions(toRemove).build());
    observer.onCompleted();
  }

  @Override
  public void syncSessions(SyncSessionsRequest request, StreamObserver<ZmqResponse> observer) {
    observer.onNext(ZmqResponse.newBuilder().build());
    observer.onCompleted();
  }

  @Override
  public void fetchConnectCommands(
      FetchConnectCommandsRequest request, StreamObserver<FetchConnectCommandsResponse> observer) {
    List<PluginInfo> pluginInfos = new ArrayList<>();
    for (String pluginName : request.getPluginNamesList()) {
      pluginInfos.add(
          PluginInfo.newBuilder()
              .setPluginName(pluginName)
              .addJoinCommands(PluginCommand.JOIN.getCode())
              .build());
    }
    observer.onNext(FetchConnectCommandsResponse.newBuilder().addAllPlugins(pluginInfos).build());
    observer.onCompleted();
  }

  @Override
  public void callBatchInternally(
      BatchPluginRequest request, StreamObserver<BatchPluginResponse> observer) {
    List<PluginResponse> responses = new ArrayList<>();
    for (PluginRequest pluginRequest : request.getRequestsList()) {
      responses.add(PluginResponse.newBuilder().build());
    }
    observer.onNext(BatchPluginResponse.newBuilder().addAllResponses(responses).build());
    observer.onCompleted();
  }

  @Override
  public void interop(InteropRequest request, StreamObserver<InteropResponse> observer) {
    observer.onNext(InteropResponse.newBuilder().setSuccess(true).build());
    observer.onCompleted();
  }

  // ─────────────────────────────────────────────
  // Internal helpers
  // ─────────────────────────────────────────────

  /**
   * Extracts cmd code from payload; handles both int and string values (be-wsproxy may encode cmd
   * as string when forwarding from WebSocket JSON frames).
   */
  private int resolveCmdCode(Map<String, Object> payload) {
    // Standard format: {"cmd": 1005} or {"cmd": "1005"}
    log.debug("[gRPC] resolveCmdCode | keys={}", payload.keySet());
    Integer topLevel = parseCmdValue(payload.get("cmd"));
    if (topLevel != null) {
      log.debug("[gRPC] resolveCmdCode | resolved from top-level cmd={}", topLevel);
      return topLevel;
    }

    log.warn(
        "[gRPC] Missing cmd in payload. topLevelKeys={} payload={}", payload.keySet(), payload);
    return -1;
  }

  private Integer parseCmdValue(Object raw) {
    if (raw instanceof Number n) {
      return n.intValue();
    }
    if (raw instanceof String s) {
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private byte[] routeToHandler(
      PluginCommand command,
      String connectionId,
      String sessionId,
      String zoneId,
      Map<String, Object> payload)
      throws Exception {

    log.info(
        "[gRPC] routeToHandler | command={} connectionId={} sessionId={} zoneId={} payloadKeys={}",
        command,
        connectionId,
        sessionId,
        zoneId,
        payload.keySet());

    return switch (command) {
      case PING ->
          MessagePackHelper.encodeResponse(PluginCommand.PING.getCode(), Map.of("pong", true));

      case JOIN -> connectHandler.handle(connectionId, sessionId, zoneId, payload);

      case SPIN -> spinHandler.handleSpin(sessionId, payload);

      case BUY_FEATURE -> spinHandler.handleBuyFeature(sessionId, payload);

      case LAST_SESSION -> {
        String agencyId =
            payloadString(payload, "agency_id", "agencyId", "agent_id", "agentId", "");
        String userId = payloadString(payload, "user_id", "userId", "user_id", "userId", "");
        String gameId =
            payloadString(payload, "game_id", "gameId", "game_id", "gameId", "nagas_treasure");
        log.info(
            "[gRPC] routeToHandler | LAST_SESSION agencyId={} userId={} gameId={}",
            agencyId,
            userId,
            gameId);
        yield spinHandler.handleLastSession(agencyId, userId, gameId, sessionId);
      }

      case GET_BALANCE -> {
        String agencyId =
            payloadString(payload, "agency_id", "agencyId", "agent_id", "agentId", "");
        String userId = payloadString(payload, "user_id", "userId", "user_id", "userId", "");
        log.info("[gRPC] routeToHandler | GET_BALANCE agencyId={} userId={}", agencyId, userId);
        yield MessagePackHelper.encodeResponse(
            PluginCommand.GET_BALANCE.getCode(),
            Map.of(
                "agency_id",
                agencyId,
                "user_id",
                userId,
                "balance",
                balanceAsDouble(agencyId, userId)));
      }

      case GET_SPIN_LIST -> {
        String agencyId =
            payloadString(payload, "agency_id", "agencyId", "agent_id", "agentId", "");
        String userId = payloadString(payload, "user_id", "userId", "user_id", "userId", "");
        String gameId = payloadString(payload, "game_id", "gameId", "game_id", "gameId", "");
        int limit = intValue(payload.get("limit"), 20);
        int offset = intValue(payload.get("offset"), 0);
        log.info(
            "[gRPC] routeToHandler | GET_SPIN_LIST agencyId={} userId={} gameId={} limit={} offset={}",
            agencyId,
            userId,
            gameId,
            limit,
            offset);
        yield MessagePackHelper.encodeResponse(
            PluginCommand.GET_SPIN_LIST.getCode(),
            Map.of(
                "spins",
                toHistoryRows(
                    slotHistoryPort.findByUser(agencyId, userId, gameId, limit, offset))));
      }

      case GET_PREV_SPIN -> {
        String agencyId =
            payloadString(payload, "agency_id", "agencyId", "agent_id", "agentId", "");
        String roundId = String.valueOf(payload.getOrDefault("roundId", ""));
        log.info("[gRPC] routeToHandler | GET_PREV_SPIN agencyId={} roundId={}", agencyId, roundId);
        if (roundId.isBlank()) {
          throw new IllegalArgumentException("MISSING_ROUND_ID");
        }
        SlotHistory history =
            slotHistoryPort
                .findByRoundId(agencyId, roundId)
                .orElseThrow(() -> new IllegalArgumentException("ROUND_NOT_FOUND"));
        yield MessagePackHelper.encodeResponse(
            PluginCommand.GET_PREV_SPIN.getCode(), historyRow(history));
      }

      default -> throw new IllegalArgumentException("Unsupported command: " + command);
    };
  }

  private ZmqResponse zmqResponseWithTopic(String topic) {
    return ZmqResponse.newBuilder()
        .setMetadata(ZmqMetadata.newBuilder().addTopics(topic).build())
        .build();
  }

  private double balanceAsDouble(String agencyId, String userId) {
    return walletPort == null ? 0.0 : walletPort.getBalance(agencyId, userId) / 100.0;
  }

  private int intValue(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String string) {
      try {
        return Integer.parseInt(string);
      } catch (NumberFormatException ignored) {
        return fallback;
      }
    }
    return fallback;
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

  private List<Map<String, Object>> toHistoryRows(List<SlotHistory> histories) {
    List<Map<String, Object>> rows = new ArrayList<>();
    if (histories == null) {
      return rows;
    }
    for (SlotHistory history : histories) {
      rows.add(historyRow(history));
    }
    return rows;
  }

  private Map<String, Object> historyRow(SlotHistory history) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("roundId", history.getRoundId());
    row.put("gameId", history.getGameId());
    row.put("betAmount", toDouble(history.getBetAmount()));
    row.put("totalWin", toDouble(history.getTotalWin()));
    row.put("timestamp", history.getTimestamp() != null ? history.getTimestamp().toString() : null);
    row.put("mode", String.valueOf(history.getThisMode()));
    return row;
  }

  private double toDouble(BigDecimal value) {
    return value == null ? 0.0 : value.doubleValue();
  }

  private void publishErrorAndComplete(
      byte[] rawData,
      int errorCode,
      String msg,
      String zone,
      String sessionId,
      StreamObserver<ZmqResponse> observer) {
    try {
      Map<String, Object> decoded = MessagePackHelper.decode(rawData);
      int cmdCode = resolveCmdCode(decoded);
      log.warn(
          "[gRPC] publishErrorAndComplete | errorCode={} msg={} session={} zone={} cmd={} decodedKeys={}",
          errorCode,
          msg,
          sessionId,
          zone,
          cmdCode,
          decoded.keySet());
      byte[] errBytes = MessagePackHelper.encodeError(cmdCode, errorCode, msg);
      String topic = ZmqTopicHelper.buildTopic(zone, sessionId);
      zmqPublisher.publish(topic, errBytes);
      log.warn(
          "[gRPC] publishErrorAndComplete | published error topic={} session={}", topic, sessionId);
      observer.onNext(zmqResponseWithTopic(topic));
    } catch (Exception ex) {
      log.error(
          "[gRPC] Failed to publish error response | session={} zone={} errorCode={} msg={} cause={}",
          sessionId,
          zone,
          errorCode,
          msg,
          ex.getMessage(),
          ex);
      observer.onNext(ZmqResponse.newBuilder().build());
    }
    observer.onCompleted();
  }

  private void sendCallError(
      byte[] rawData, int errorCode, String msg, StreamObserver<PluginResponse> observer) {
    try {
      Map<String, Object> decoded = MessagePackHelper.decode(rawData);
      int cmdCode = resolveCmdCode(decoded);
      log.warn(
          "[gRPC] sendCallError | errorCode={} msg={} cmd={} decodedKeys={}",
          errorCode,
          msg,
          cmdCode,
          decoded.keySet());
      byte[] errBytes = MessagePackHelper.encodeError(cmdCode, errorCode, msg);
      observer.onNext(PluginResponse.newBuilder().setResult(ByteString.copyFrom(errBytes)).build());
    } catch (Exception ex) {
      log.error(
          "[gRPC] Failed to encode call error response | errorCode={} msg={} cause={}",
          errorCode,
          msg,
          ex.getMessage(),
          ex);
      observer.onNext(PluginResponse.newBuilder().build());
    }
    observer.onCompleted();
  }
}
