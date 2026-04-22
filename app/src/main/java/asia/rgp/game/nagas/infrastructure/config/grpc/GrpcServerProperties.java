package asia.rgp.game.nagas.infrastructure.config.grpc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the standalone gRPC plugin server.
 *
 * <p>Reads from application.yml under prefix: grpc.plugin
 *
 * <p>Example:
 *
 * <pre>
 * grpc:
 *   plugin:
 *     port: 9101
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "grpc.plugin")
public class GrpcServerProperties {

  /** Port the gRPC server listens on. WsProxy connects to this port. */
  private int port = 9101;
}
