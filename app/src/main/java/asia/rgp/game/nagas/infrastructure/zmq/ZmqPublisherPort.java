package asia.rgp.game.nagas.infrastructure.zmq;

/**
 * Port for publishing messages via ZeroMQ.
 * Used by gRPC adapter to send spin results to WS Proxy.
 */
public interface ZmqPublisherPort {

    /**
     * Publish a message to the given topic.
     *
     * @param topic   ZMQ topic (e.g., "urn:ws:z:1:s:{sessionId}")
     * @param payload MessagePack encoded payload
     */
    void publish(String topic, byte[] payload);
}
