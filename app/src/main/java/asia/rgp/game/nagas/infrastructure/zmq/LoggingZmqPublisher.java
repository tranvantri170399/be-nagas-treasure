package asia.rgp.game.nagas.infrastructure.zmq;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock ZMQ publisher that logs messages instead of publishing. Used for local testing without
 * ZeroMQ infrastructure.
 *
 * <p>To use: remove @Component from JeroMqPublisher and add @Component here.
 */
@Slf4j
public class LoggingZmqPublisher implements ZmqPublisherPort {

  @Override
  public void publish(String topic, byte[] payload) {
    log.info(
        "[zmq-mock] PUBLISH topic={} payloadSize={} bytes",
        topic,
        payload != null ? payload.length : 0);
  }
}
