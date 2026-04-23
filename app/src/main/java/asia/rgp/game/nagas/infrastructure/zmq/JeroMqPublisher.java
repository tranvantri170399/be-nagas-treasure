package asia.rgp.game.nagas.infrastructure.zmq;

import asia.rgp.game.nagas.infrastructure.grpc.MessagePackHelper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
 * <p>Frame layout (single frame, matches be-wsproxy JeroMqFrameDecoder):
 *
 * <pre>
 * [version:1][flags:1][type:1][0xD3:1][timestamp:8][topic:msgpack-str][0xC0:1][payload:n]
 * </pre>
 */
@Slf4j
@Component
public class JeroMqPublisher implements ZmqPublisherPort {

  private static final byte METADATA_VERSION = 1;
  private static final byte METADATA_FLAGS = 0;
  private static final byte TYPE_PU_ELEMENT = 12;

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

  private byte[] buildFrame(String topic, byte[] payload) {
    byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
    int metadataSize =
        3 // version + flags + type
            + 1
            + 8 // 0xD3 + timestamp (int64)
            + msgpackStringSize(topicBytes) // topic
            + 1; // 0xC0 (nil headers)

    byte[] frame = new byte[metadataSize + payload.length];
    ByteBuffer buf = ByteBuffer.wrap(frame);

    buf.put(METADATA_VERSION);
    buf.put(METADATA_FLAGS);
    buf.put(TYPE_PU_ELEMENT);
    buf.put((byte) 0xd3);
    buf.putLong(System.currentTimeMillis());
    writeMsgpackString(buf, topicBytes);
    buf.put((byte) 0xc0);
    buf.put(payload);

    return frame;
  }

  private int msgpackStringSize(byte[] strBytes) {
    int len = strBytes.length;
    if (len <= 31) return 1 + len;
    if (len <= 255) return 2 + len;
    if (len <= 65535) return 3 + len;
    return 5 + len;
  }

  private void writeMsgpackString(ByteBuffer buf, byte[] strBytes) {
    int len = strBytes.length;
    if (len <= 31) {
      buf.put((byte) (0xa0 | len));
    } else if (len <= 255) {
      buf.put((byte) 0xd9);
      buf.put((byte) len);
    } else if (len <= 65535) {
      buf.put((byte) 0xda);
      buf.putShort((short) len);
    } else {
      buf.put((byte) 0xdb);
      buf.putInt(len);
    }
    buf.put(strBytes);
  }
}
