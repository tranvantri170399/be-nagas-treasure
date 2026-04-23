package asia.rgp.game.nagas.infrastructure.zmq;

import com.luigi.gaas.common.data.PuElement;

/**
 * Port for publishing messages via ZeroMQ. Used by gRPC adapter to send spin results to WS Proxy.
 */
public interface ZmqPublisherPort {

  /**
   * Publish a message to the given topic.
   *
   * @param topic ZMQ topic (e.g., "urn:ws:z:1:s:{sessionId}")
   * @param payload Luigi PuElement payload
   */
  void publish(String topic, PuElement payload);
}
