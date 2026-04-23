package asia.rgp.game.nagas.infrastructure.grpc;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
public class PluginSessionStore {

  private final Map<String, SessionAuth> sessionsBySessionId = new ConcurrentHashMap<>();
  private final Map<String, String> sessionIdByUsername = new ConcurrentHashMap<>();

  public void put(String sessionId, SessionAuth auth) {
    if (sessionId == null || sessionId.isBlank() || auth == null) {
      return;
    }
    sessionsBySessionId.put(sessionId, auth);
    if (auth.getUsername() != null && !auth.getUsername().isBlank()) {
      sessionIdByUsername.put(auth.getUsername(), sessionId);
    }
  }

  public Optional<SessionAuth> get(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(sessionsBySessionId.get(sessionId));
  }

  public Optional<SessionAuth> getByUsernameOrSessionId(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }

    SessionAuth bySessionId = sessionsBySessionId.get(value);
    if (bySessionId != null) {
      return Optional.of(bySessionId);
    }

    String sessionId = sessionIdByUsername.get(value);
    if (sessionId == null || sessionId.isBlank()) {
      return Optional.empty();
    }

    return Optional.ofNullable(sessionsBySessionId.get(sessionId));
  }

  public Optional<SessionAuth> remove(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return Optional.empty();
    }
    SessionAuth removed = sessionsBySessionId.remove(sessionId);
    if (removed != null && removed.getUsername() != null && !removed.getUsername().isBlank()) {
      sessionIdByUsername.remove(removed.getUsername(), sessionId);
    }
    return Optional.ofNullable(removed);
  }

  @Value
  @Builder
  public static class SessionAuth {
    String sessionId;
    String userId;
    String username;
    String agency;
    String token;
    String zone;
    String pluginName;
  }
}
