package asia.rgp.game.nagas.infrastructure.config.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Standalone gRPC server lifecycle manager.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Start gRPC server on @PostConstruct (app startup)
 *   <li>Gracefully stop on @PreDestroy (app shutdown)
 *   <li>Auto-register all {@link BindableService} beans found in Spring context
 * </ul>
 *
 * <p>Port is read from: {@code grpc.plugin.port} in application.yml
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(GrpcServerProperties.class)
public class StandaloneGrpcServerConfig {

  private final GrpcServerProperties properties;
  private final List<BindableService> services;

  private Server server;

  @PostConstruct
  public void start() {
    ServerBuilder<?> builder = ServerBuilder.forPort(properties.getPort());
    services.forEach(
        service -> {
          builder.addService(service);
          log.info("[gRPC] Registered service: {}", service.getClass().getSimpleName());
        });

    try {
      server = builder.build().start();
      log.info("[gRPC] Standalone server started on port {}", properties.getPort());
    } catch (IOException e) {
      throw new IllegalStateException(
          "[gRPC] Failed to start server on port " + properties.getPort(), e);
    }
  }

  @PreDestroy
  public void stop() {
    if (server == null || server.isShutdown()) {
      return;
    }
    log.info("[gRPC] Shutting down server...");
    server.shutdown();
    try {
      if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
        log.warn("[gRPC] Forced shutdown after timeout");
        server.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      server.shutdownNow();
    }
    log.info("[gRPC] Server stopped");
  }
}
