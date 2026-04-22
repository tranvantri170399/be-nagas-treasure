package asia.rgp.game.nagas.infrastructure.grpc.handler;

import asia.rgp.game.nagas.infrastructure.grpc.MessagePackHelper;
import asia.rgp.game.nagas.infrastructure.grpc.PluginCommand;
import asia.rgp.game.nagas.infrastructure.grpc.ZmqTopicHelper;
import asia.rgp.game.nagas.infrastructure.grpc.generated.ConnectAndCallRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.DisconnectRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.PluginRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.PluginResponse;
import asia.rgp.game.nagas.infrastructure.grpc.generated.PluginServiceGrpc;
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

  // ─────────────────────────────────────────────
  // gRPC Methods
  // ─────────────────────────────────────────────

  @Override
  public void connectAndCall(ConnectAndCallRequest request, StreamObserver<ZmqResponse> observer) {
    String zone = request.getZone();
    String sessionId = request.getUser().getSessionId();
    String userId = request.getUser().getId();
    byte[] rawData = request.getData().toByteArray();

    try {
      Map<String, Object> payload = MessagePackHelper.decode(rawData);
      log.info(
          "[gRPC] ConnectAndCall | rawDataBytes={} decodedPayload={}", rawData.length, payload);
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

      byte[] responseData = routeToHandler(command, sessionId, sessionId, zone, payload);

      String topic = ZmqTopicHelper.buildTopic(zone, sessionId);
      zmqPublisher.publish(topic, responseData);

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

  @Override
  public void call(PluginRequest request, StreamObserver<PluginResponse> observer) {
    String zone = request.getZone();
    String sessionId = request.getUsername(); // be-wsproxy puts sessionId in username
    byte[] rawData = request.getData().toByteArray();

    try {
      Map<String, Object> payload = MessagePackHelper.decode(rawData);
      log.info("[gRPC] Call | rawDataBytes={} decodedPayload={}", rawData.length, payload);
      int cmdCode = resolveCmdCode(payload);

      log.info("[gRPC] Call | cmd={} session={} zone={}", cmdCode, sessionId, zone);

      PluginCommand command =
          PluginCommand.fromCode(cmdCode)
              .orElseThrow(() -> new IllegalArgumentException("Unknown cmd: " + cmdCode));

      byte[] responseData = routeToHandler(command, sessionId, sessionId, zone, payload);

      String topic = ZmqTopicHelper.buildTopic(zone, sessionId);
      zmqPublisher.publish(topic, responseData);

      observer.onNext(
          PluginResponse.newBuilder().setResult(ByteString.copyFrom(responseData)).build());
      observer.onCompleted();

    } catch (IllegalArgumentException e) {
      log.warn("[gRPC] Call bad request session={}: {}", sessionId, e.getMessage());
      sendCallError(rawData, 400, e.getMessage(), observer);
    } catch (Exception e) {
      log.error("[gRPC] Call error session={}: {}", sessionId, e.getMessage(), e);
      sendCallError(rawData, 500, "Internal server error", observer);
    }
  }

  @Override
  public void disconnect(DisconnectRequest request, StreamObserver<ZmqResponse> observer) {
    log.info("[gRPC] Disconnect | session={} zone={}", request.getSessionId(), request.getZone());
    observer.onNext(ZmqResponse.newBuilder().build());
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
    Integer resolved = findCmdValue(payload);
    if (resolved != null) {
      return resolved;
    }

    log.warn(
        "[gRPC] Missing cmd in payload. topLevelKeys={} payload={}", payload.keySet(), payload);
    return -1;
  }

  private Integer findCmdValue(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof Map<?, ?> map) {
      Integer directCmd = parseCmdValue(map.get("cmd"));
      if (directCmd != null) {
        return directCmd;
      }

      for (Object nestedValue : map.values()) {
        Integer nestedCmd = findCmdValue(nestedValue);
        if (nestedCmd != null) {
          return nestedCmd;
        }
      }
      return null;
    }

    if (value instanceof List<?> list) {
      for (Object item : list) {
        Integer nestedCmd = findCmdValue(item);
        if (nestedCmd != null) {
          return nestedCmd;
        }
      }
    }

    return parseCmdValue(value);
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

    return switch (command) {
      case PING ->
          MessagePackHelper.encodeResponse(PluginCommand.PING.getCode(), Map.of("pong", true));

      case JOIN -> connectHandler.handle(connectionId, sessionId, zoneId, payload);

      case SPIN -> spinHandler.handleSpin(sessionId, payload);

      case BUY_FEATURE -> spinHandler.handleBuyFeature(sessionId, payload);

      case LAST_SESSION -> {
        String agentId = (String) payload.getOrDefault("agent_id", "");
        String userId = (String) payload.getOrDefault("user_id", "");
        String gameId = (String) payload.getOrDefault("game_id", "nagas_treasure");
        yield spinHandler.handleLastSession(agentId, userId, gameId, sessionId);
      }

      case GET_BALANCE -> {
        String agentId = (String) payload.getOrDefault("agent_id", "");
        String userId = (String) payload.getOrDefault("user_id", "");
        yield MessagePackHelper.encodeResponse(
            PluginCommand.GET_BALANCE.getCode(),
            Map.of(
                "agent_id",
                agentId,
                "user_id",
                userId,
                "balance",
                balanceAsDouble(agentId, userId)));
      }

      case GET_SPIN_LIST -> {
        String agentId = (String) payload.getOrDefault("agent_id", "");
        String userId = (String) payload.getOrDefault("user_id", "");
        String gameId = (String) payload.getOrDefault("game_id", "");
        int limit = intValue(payload.get("limit"), 20);
        int offset = intValue(payload.get("offset"), 0);
        yield MessagePackHelper.encodeResponse(
            PluginCommand.GET_SPIN_LIST.getCode(),
            Map.of(
                "spins",
                toHistoryRows(slotHistoryPort.findByUser(agentId, userId, gameId, limit, offset))));
      }

      case GET_PREV_SPIN -> {
        String agentId = (String) payload.getOrDefault("agent_id", "");
        String roundId = String.valueOf(payload.getOrDefault("roundId", ""));
        if (roundId.isBlank()) {
          throw new IllegalArgumentException("MISSING_ROUND_ID");
        }
        SlotHistory history =
            slotHistoryPort
                .findByRoundId(agentId, roundId)
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

  private double balanceAsDouble(String agentId, String userId) {
    return walletPort == null ? 0.0 : walletPort.getBalance(agentId, userId) / 100.0;
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
      byte[] errBytes = MessagePackHelper.encodeError(cmdCode, errorCode, msg);
      String topic = ZmqTopicHelper.buildTopic(zone, sessionId);
      zmqPublisher.publish(topic, errBytes);
      observer.onNext(zmqResponseWithTopic(topic));
    } catch (Exception ex) {
      log.error("[gRPC] Failed to publish error response: {}", ex.getMessage());
      observer.onNext(ZmqResponse.newBuilder().build());
    }
    observer.onCompleted();
  }

  private void sendCallError(
      byte[] rawData, int errorCode, String msg, StreamObserver<PluginResponse> observer) {
    try {
      Map<String, Object> decoded = MessagePackHelper.decode(rawData);
      int cmdCode = resolveCmdCode(decoded);
      byte[] errBytes = MessagePackHelper.encodeError(cmdCode, errorCode, msg);
      observer.onNext(PluginResponse.newBuilder().setResult(ByteString.copyFrom(errBytes)).build());
    } catch (Exception ex) {
      log.error("[gRPC] Failed to encode call error response: {}", ex.getMessage());
      observer.onNext(PluginResponse.newBuilder().build());
    }
    observer.onCompleted();
  }
}
