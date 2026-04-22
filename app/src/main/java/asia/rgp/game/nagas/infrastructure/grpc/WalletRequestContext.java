package asia.rgp.game.nagas.infrastructure.grpc;

import java.util.Optional;
import lombok.Builder;
import lombok.Value;

public final class WalletRequestContext {

  private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

  private WalletRequestContext() {}

  public static void set(Context context) {
    CURRENT.set(context);
  }

  public static Optional<Context> get() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static void clear() {
    CURRENT.remove();
  }

  @Value
  @Builder
  public static class Context {
    String sessionId;
    String userId;
    String agency;
    String token;
  }
}
