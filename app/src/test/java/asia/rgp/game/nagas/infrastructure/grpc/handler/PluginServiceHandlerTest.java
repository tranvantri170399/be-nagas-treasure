package asia.rgp.game.nagas.infrastructure.grpc.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import asia.rgp.game.nagas.infrastructure.grpc.PluginSessionStore;
import asia.rgp.game.nagas.infrastructure.grpc.PluginSessionStore.SessionAuth;
import asia.rgp.game.nagas.infrastructure.grpc.generated.PluginRequest;
import asia.rgp.game.nagas.infrastructure.grpc.generated.PluginResponse;
import asia.rgp.game.nagas.infrastructure.zmq.ZmqPublisherPort;
import asia.rgp.game.nagas.modules.slot.application.port.out.JackpotHistoryPort;
import asia.rgp.game.nagas.modules.slot.application.port.out.SlotHistoryPort;
import asia.rgp.game.nagas.modules.slot.application.port.out.WalletPort;
import asia.rgp.game.nagas.modules.slot.domain.model.JackpotHistory;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotHistory;
import com.google.protobuf.ByteString;
import com.luigi.gaas.common.data.PuObject;
import com.luigi.gaas.common.data.msgpkg.MarioBytesCodec;
import io.grpc.stub.StreamObserver;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PluginServiceHandlerTest {

  @Mock private SpinHandler spinHandler;
  @Mock private ConnectHandler connectHandler;
  @Mock private ZmqPublisherPort zmqPublisher;
  @Mock private WalletPort walletPort;
  @Mock private JackpotHistoryPort jackpotHistoryPort;
  @Mock private SlotHistoryPort slotHistoryPort;
  @Mock private StreamObserver<PluginResponse> observer;

  private PluginSessionStore sessionStore;
  private PluginServiceHandler handler;

  @BeforeEach
  void setUp() {
    sessionStore = new PluginSessionStore();
    handler =
        new PluginServiceHandler(
            spinHandler,
            connectHandler,
            zmqPublisher,
            walletPort,
            jackpotHistoryPort,
            slotHistoryPort,
            sessionStore);

    sessionStore.put(
        "session-1",
        SessionAuth.builder()
            .sessionId("session-1")
            .username("player-1")
            .userId("user-1")
            .agency("agency-1")
            .token("token-1")
            .zone("MiniGame")
            .pluginName("game-nagas-treasure")
            .build());
  }

  @Test
  @DisplayName("GET_SPIN_LIST should query history list and publish response")
  void callGetSpinList_shouldQueryHistoryAndPublish() throws Exception {
    when(slotHistoryPort.findByUser("agency-1", "user-1", "nagas_treasure", 10, 5))
        .thenReturn(List.of(sampleHistory("round-100")));

    PluginRequest request =
        pluginRequest(
            "player-1",
            Map.of(
                "cmd", 1504,
                "game_id", "nagas_treasure",
                "limit", 10,
                "offset", 5));

    handler.call(request, observer);

    verify(slotHistoryPort).findByUser("agency-1", "user-1", "nagas_treasure", 10, 5);
    verify(zmqPublisher).publish(eq("urn:ws:z:MiniGame:s:session-1"), any());

    ArgumentCaptor<PluginResponse> responseCaptor = ArgumentCaptor.forClass(PluginResponse.class);
    verify(observer).onNext(responseCaptor.capture());
    verify(observer).onCompleted();
    assertEquals(0, responseCaptor.getValue().getResult().size());
  }

  @Test
  @DisplayName("GET_PREV_SPIN should query history detail and publish response")
  void callGetPrevSpin_shouldQueryDetailAndPublish() throws Exception {
    when(slotHistoryPort.findByRoundId("agency-1", "round-777"))
        .thenReturn(Optional.of(sampleHistory("round-777")));

    PluginRequest request = pluginRequest("player-1", Map.of("cmd", 1508, "roundId", "round-777"));

    handler.call(request, observer);

    verify(slotHistoryPort).findByRoundId("agency-1", "round-777");
    verify(zmqPublisher).publish(eq("urn:ws:z:MiniGame:s:session-1"), any());

    ArgumentCaptor<PluginResponse> responseCaptor = ArgumentCaptor.forClass(PluginResponse.class);
    verify(observer).onNext(responseCaptor.capture());
    verify(observer).onCompleted();
    assertEquals(0, responseCaptor.getValue().getResult().size());
  }

  @Test
  @DisplayName("GET_PREV_SPIN should return 400 when roundId is missing")
  void callGetPrevSpin_missingRoundId_shouldReturnError() throws Exception {
    PluginRequest request = pluginRequest("player-1", Map.of("cmd", 1508));

    handler.call(request, observer);

    verify(slotHistoryPort, never()).findByRoundId(any(), any());
    verify(zmqPublisher, never()).publish(anyString(), any());

    ArgumentCaptor<PluginResponse> responseCaptor = ArgumentCaptor.forClass(PluginResponse.class);
    verify(observer).onNext(responseCaptor.capture());
    verify(observer).onCompleted();

    assertTrue(responseCaptor.getValue().getResult().size() > 0);
  }

  @Test
  @DisplayName("JACKPOT_HISTORY should query jackpot history list and publish response")
  void callJackpotHistory_shouldQueryHistoryAndPublish() throws Exception {
    when(jackpotHistoryPort.findByAgencyId("agency-1", 25))
        .thenReturn(List.of(sampleJackpotHistory("jp-100")));

    PluginRequest request = pluginRequest("player-1", Map.of("cmd", 1507, "limit", 25));

    handler.call(request, observer);

    verify(jackpotHistoryPort).findByAgencyId("agency-1", 25);
    verify(zmqPublisher).publish(eq("urn:ws:z:MiniGame:s:session-1"), any());

    ArgumentCaptor<PluginResponse> responseCaptor = ArgumentCaptor.forClass(PluginResponse.class);
    verify(observer).onNext(responseCaptor.capture());
    verify(observer).onCompleted();
    assertEquals(0, responseCaptor.getValue().getResult().size());
  }

  @Test
  @DisplayName("GET_PREV_SPIN should return 400 when round is not found")
  void callGetPrevSpin_roundNotFound_shouldReturnError() throws Exception {
    when(slotHistoryPort.findByRoundId("agency-1", "round-not-found")).thenReturn(Optional.empty());

    PluginRequest request =
        pluginRequest("player-1", Map.of("cmd", 1508, "roundId", "round-not-found"));

    handler.call(request, observer);

    verify(slotHistoryPort).findByRoundId("agency-1", "round-not-found");
    verify(zmqPublisher, never()).publish(anyString(), any());

    ArgumentCaptor<PluginResponse> responseCaptor = ArgumentCaptor.forClass(PluginResponse.class);
    verify(observer).onNext(responseCaptor.capture());
    verify(observer).onCompleted();

    assertTrue(responseCaptor.getValue().getResult().size() > 0);
  }

  private PluginRequest pluginRequest(String username, Map<String, Object> payload)
      throws Exception {
    return PluginRequest.newBuilder()
        .setZone("MiniGame")
        .setUsername(username)
        .setData(ByteString.copyFrom(pack(payload)))
        .build();
  }

  private byte[] pack(Map<String, Object> payload) throws Exception {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      MarioBytesCodec.pack(baos, PuObject.fromObject(payload));
      return baos.toByteArray();
    }
  }

  private SlotHistory sampleHistory(String roundId) {
    return SlotHistory.builder()
        .roundId(roundId)
        .agencyId("agency-1")
        .userId("user-1")
        .gameId("nagas_treasure")
        .sessionId("session-1")
        .thisMode("base")
        .nextMode("base")
        .betAmount(BigDecimal.ONE)
        .totalWin(BigDecimal.ZERO)
        .timestamp(Instant.parse("2026-04-28T04:00:00Z"))
        .build();
  }

  private JackpotHistory sampleJackpotHistory(String id) {
    return JackpotHistory.builder()
        .id(id)
        .winId("win-1")
        .agencyId("agency-1")
        .userId("user-1")
        .username("player-1")
        .sessionId("session-1")
        .jackpotType("GRAND")
        .amount(12345.67)
        .createdAt(Instant.parse("2026-04-28T05:00:00Z"))
        .build();
  }
}
