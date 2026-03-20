package asia.rgp.game.nagas.infrastructure.zmq;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * Real ZMQ publisher using JeroMQ PUB socket.
 * Connects to WS Proxy SUB socket to publish game results.
 *
 * Pattern: Game Backend (PUB) → connect → WS Proxy (SUB bind)
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
            // ZMQ PUB sends: [topic] [payload] as multipart
            pubSocket.sendMore(topic);
            pubSocket.send(payload);
            log.info("[zmq] PUBLISHED topic={} payloadSize={} bytes", topic, payload.length);
        } catch (Exception e) {
            log.error("[zmq] Failed to publish topic={}: {}", topic, e.getMessage(), e);
        }
    }
}
