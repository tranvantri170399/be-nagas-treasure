package asia.rgp.game.nagas.shared.application.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import asia.rgp.game.nagas.infrastructure.cache.HotCacheService;
import asia.rgp.game.nagas.shared.application.exception.ApplicationException;
import asia.rgp.game.nagas.shared.error.ErrorCode;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final HotCacheService hotCache;

    public long increment(String key, Duration ttl) {
        try {
            long nextValue = hotCache.increment(key);
            hotCache.expire(key, ttl);
            
            log.debug("[counter] incremented key={} to value={}", key, nextValue);
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
        log.debug("[lock] attempting acquire lockKey={}, token={}, ttl={}", lockKey, token, ttl);
        boolean acquired = hotCache.putIfAbsent(lockKey, token, ttl);
        log.debug("[lock] acquire result for lockKey={} token={} -> {}", lockKey, token, acquired);

        if (!acquired) {
            log.warn("[lock] failed to acquire lock for key: {} (lock may be held by another request)", key);
            throw new ApplicationException(
                    "Could not acquire lock for key: " + key + ". Another request may be processing. Please try again.",
                    ErrorCode.LOCK_ACQUISITION_FAILED);
        }

        try {
            return action.get();
        } catch (RuntimeException ex) {
            throw ex;
        } finally {
            try {
                hotCache.evict(lockKey);
                log.debug("[lock] released lockKey={} token={}", lockKey, token);
            } catch (Exception e) {
                log.warn("[lock] failed to evict lockKey={} token={} - {}", lockKey, token, e.toString());
            }
        }
    }

    public void withLockVoid(String key, Duration ttl, Runnable action) {
        withLock(key, ttl, () -> {
            action.run();
            return null;
        });
    }

    public <T> T withLockRetry(String key, Duration ttl, int maxRetries, long retryDelay, Supplier<T> action) {
        String lockKey = buildLockKey(key);
        String token = UUID.randomUUID().toString();
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            boolean acquired = hotCache.putIfAbsent(lockKey, token, ttl);
            
            if (acquired) {
                try {
                    return action.get();
                } finally {
                    hotCache.evict(lockKey);
                }
            }
            
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ApplicationException("Lock acquisition interrupted", ErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
        }
        
        throw new ApplicationException(
                "Could not acquire lock for key: " + key + " after " + (maxRetries + 1) + " attempts.",
                ErrorCode.LOCK_ACQUISITION_FAILED);
    }

    private String buildLockKey(String key) {
        return "lock:" + key;
    }
}