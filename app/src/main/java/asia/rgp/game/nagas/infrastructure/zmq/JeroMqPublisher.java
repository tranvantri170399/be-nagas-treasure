package asia.rgp.game.nagas.infrastructure.zmq;

import com.luigi.gaas.common.data.PuElement;
import com.luigi.gaas.common.data.msgpkg.v1.Metadata;
import com.luigi.gaas.common.data.msgpkg.v1.PuElementMessageFrame;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
  public synchronized void publish(String topic, PuElement payload) {
    try {
      if (payload == null) {
        throw new IllegalArgumentException("Payload must not be null");
      }
      log.info("[zmq] PREPARE topic={} payload={}", topic, payload);
      PuElementMessageFrame messageFrame =
          new PuElementMessageFrame(new Metadata(topic, null), payload);
      byte[] frame = messageFrame.toBytes();
      pubSocket.send(frame);
      log.info("[zmq] PUBLISHED topic={} frameSize={} bytes", topic, frame.length);
    } catch (Exception e) {
      log.error("[zmq] Failed to publish topic={}: {}", topic, e.getMessage(), e);
    }
  }
}
