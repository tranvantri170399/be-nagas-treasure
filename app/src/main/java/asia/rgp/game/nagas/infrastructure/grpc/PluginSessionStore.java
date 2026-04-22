package asia.rgp.game.nagas.infrastructure.grpc;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
public class PluginSessionStore {

  private final Map<String, SessionAuth> sessions = new ConcurrentHashMap<>();

  public void put(String sessionId, SessionAuth auth) {
    if (sessionId == null || sessionId.isBlank() || auth == null) {
      return;
    }
    sessions.put(sessionId, auth);
  }

  public Optional<SessionAuth> get(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(sessions.get(sessionId));
  }

  public Optional<SessionAuth> remove(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(sessions.remove(sessionId));
  }

  @Value
  @Builder
  public static class SessionAuth {
    String sessionId;
    String userId;
    String agency;
    String token;
    String zone;
    String pluginName;
  }
}
