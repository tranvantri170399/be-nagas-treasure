package asia.rgp.game.nagas.infrastructure.zmq;

import asia.rgp.game.nagas.infrastructure.grpc.MessagePackHelper;
import com.luigi.gaas.common.data.PuElement;
import com.luigi.gaas.common.data.msgpkg.v1.Metadata;
import com.luigi.gaas.common.data.msgpkg.v1.PuElementMessageFrame;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * Real ZMQ publisher using JeroMQ PUB socket. Connects to WS Proxy SUB socket to publish game
 * results.
 *
 * <p>Pattern: Game Backend (PUB) → connect → WS Proxy (SUB bind)
 *
 * <p>Frame layout is delegated to Luigi GaaS {@link PuElementMessageFrame}, matching the working
 * mania implementation.
 */
@Slf4j
@Component
public class JeroMqPublisher implements ZmqPublisherPort {

  @Value("${zmq.publisher.address}")
  private String address;

  private ZContext context;
  private ZMQ.Socket pubSocket;

  @PostConstruct
  public void init() {
    context = new ZContext();
    pubSocket = context.createSocket(SocketType.PUB);
    pubSocket.connect(address);
    log.info("[zmq] PUB socket connected to {}", address);
  }

  @PreDestroy
  public void destroy() {
    if (pubSocket != null) {
      pubSocket.close();
    }
    if (context != null) {
      context.close();
    }
    log.info("[zmq] PUB socket closed");
  }

  @Override
  public synchronized void publish(String topic, byte[] payload) {
    try {
      byte[] safePayload = payload == null ? new byte[0] : payload;
      log.info(
          "[zmq] PREPARE topic={} payloadSize={} bytes decodedPayload={}",
          topic,
          safePayload.length,
          describePayload(safePayload));
      byte[] frame = buildFrame(topic, safePayload);
      pubSocket.send(frame);
      log.info(
          "[zmq] PUBLISHED topic={} payloadSize={} bytes frameSize={} bytes",
          topic,
          safePayload.length,
          frame.length);
    } catch (Exception e) {
      log.error("[zmq] Failed to publish topic={}: {}", topic, e.getMessage(), e);
    }
  }

  private String describePayload(byte[] payload) {
    if (payload == null || payload.length == 0) {
      return "<empty>";
    }

    try {
      return String.valueOf(MessagePackHelper.decode(payload));
    } catch (Exception e) {
      return "<unreadable: " + e.getMessage() + ">";
    }
  }

  private byte[] buildFrame(String topic, byte[] payload) throws IOException {
    PuElement puPayload = MessagePackHelper.unpackPuElement(payload);
    PuElementMessageFrame messageFrame =
        new PuElementMessageFrame(new Metadata(topic, null), puPayload);
    return messageFrame.toBytes();
  }
}
