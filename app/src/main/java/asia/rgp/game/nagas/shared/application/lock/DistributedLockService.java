package asia.rgp.game.nagas.shared.application.lock;

import asia.rgp.game.nagas.infrastructure.cache.HotCacheService;
import asia.rgp.game.nagas.shared.application.exception.ApplicationException;
import asia.rgp.game.nagas.shared.error.ErrorCode;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

  private final HotCacheService hotCache;

  public long increment(String key, Duration ttl) {
    try {
      long nextValue = hotCache.increment(key);
      hotCache.expire(key, ttl);
      return nextValue;
    } catch (Exception e) {
      log.error("[counter] failed to increment key={}: {}", key, e.getMessage());
      return System.currentTimeMillis() % 10000;
    }
  }

  public long increment(String key) {
    return increment(key, Duration.ofDays(1));
  }

  public <T> T withLock(String key, Duration ttl, Supplier<T> action) {
    String lockKey = buildLockKey(key);
    String token = UUID.randomUUID().toString();

    boolean acquired = hotCache.putIfAbsent(lockKey, token, ttl);

    if (!acquired) {
      log.warn("[lock] failed to acquire lock for key: {} (already held)", key);
      throw new ApplicationException(
          "Another request is processing. Please wait.", ErrorCode.LOCK_ACQUISITION_FAILED);
    }

    try {
      return action.get();
    } finally {
      try {
        boolean released = hotCache.evictIfValueMatches(lockKey, token);
        log.debug("[lock] release result for lockKey={} token={} -> {}", lockKey, token, released);
      } catch (Exception e) {
        log.warn("[lock] failed to safely release lockKey={}: {}", lockKey, e.getMessage());
      }
    }
  }

  public <T> T withLockRetry(
      String key, Duration ttl, int maxRetries, long retryDelay, Supplier<T> action) {
    String lockKey = buildLockKey(key);
    String token = UUID.randomUUID().toString();

    for (int attempt = 0; attempt <= maxRetries; attempt++) {
      boolean acquired = hotCache.putIfAbsent(lockKey, token, ttl);

      if (acquired) {
        try {
          return action.get();
        } finally {
          hotCache.evictIfValueMatches(lockKey, token);
        }
      }

      if (attempt < maxRetries) {
        try {
          Thread.sleep(retryDelay);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new ApplicationException("Lock interrupted", ErrorCode.INTERNAL_SERVER_ERROR);
        }
      }
    }

    throw new ApplicationException(
        "Could not acquire lock after retries", ErrorCode.LOCK_ACQUISITION_FAILED);
  }

  private String buildLockKey(String key) {
    return "lock:" + key;
  }
}
