package asia.rgp.game.nagas.infrastructure.zmq;

import com.luigi.gaas.common.data.PuElement;
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
  public void publish(String topic, PuElement payload) {
    log.info("[zmq-mock] PUBLISH topic={} payload={}", topic, payload);
  }
}
