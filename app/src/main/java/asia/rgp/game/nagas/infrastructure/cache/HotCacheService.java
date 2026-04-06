package asia.rgp.game.nagas.infrastructure.cache;

import asia.rgp.game.nagas.infrastructure.config.redis.HotCacheProperties;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "deprecation"})
public class HotCacheService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final HotCacheProperties hotCacheProperties;

  // --- VALUE OPERATIONS (String/Object) ---

  public void put(String cacheKey, Object value) {
    put(cacheKey, value, hotCacheProperties.getDefaultTtl());
  }

  public void put(String cacheKey, Object value, Duration ttl) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    Duration effectiveTtl = ttl != null ? ttl : hotCacheProperties.getDefaultTtl();
    redisTemplate.opsForValue().set(buildKey(cacheKey), value, effectiveTtl);
  }

  public boolean putIfAbsent(String cacheKey, Object value, Duration ttl) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    Duration effectiveTtl = ttl != null ? ttl : hotCacheProperties.getDefaultTtl();
    Boolean result =
        redisTemplate.opsForValue().setIfAbsent(buildKey(cacheKey), value, effectiveTtl);
    return Boolean.TRUE.equals(result);
  }

  public <T> Optional<T> get(String cacheKey, Class<T> type) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    Object cached = redisTemplate.opsForValue().get(buildKey(cacheKey));
    if (cached == null) return Optional.empty();

    try {
      return Optional.of(type.cast(cached));
    } catch (ClassCastException ex) {
      log.warn("Cache type mismatch for key {}. Evicting.", cacheKey);
      evict(cacheKey);
      return Optional.empty();
    }
  }

  public long increment(String cacheKey) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    String redisKey = buildKey(cacheKey);
    Long result = redisTemplate.opsForValue().increment(redisKey);
    return result != null ? result : 0L;
  }

  public Double incrementHash(String cacheKey, String hashKey, double delta) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    Assert.hasText(hashKey, "hashKey must not be blank");
    return redisTemplate.opsForHash().increment(buildKey(cacheKey), hashKey, delta);
  }

  public Object getHash(String cacheKey, String hashKey) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    return redisTemplate.opsForHash().get(buildKey(cacheKey), hashKey);
  }

  public void putHash(String cacheKey, String hashKey, Object value) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    Object valueToStore = (value instanceof Number) ? String.valueOf(value) : value;
    redisTemplate.opsForHash().put(buildKey(cacheKey), hashKey, valueToStore);
  }

  public Map<Object, Object> entries(String cacheKey) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    return redisTemplate.opsForHash().entries(buildKey(cacheKey));
  }

  // --- ATOMIC HASH OPERATIONS ---

  /**
   * Atomically reads a hash field value and resets it to a new value. Returns the value BEFORE
   * reset. Used for jackpot pool claims to prevent double-win race conditions.
   *
   * <p>Lua script executes as a single atomic operation on Redis server: no other command can
   * interleave between the read and write.
   *
   * @return the pool amount before reset, or null if field doesn't exist
   */
  public String getAndResetHash(String cacheKey, String hashKey, String resetValue) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    Assert.hasText(hashKey, "hashKey must not be blank");

    final String redisKey = buildKey(cacheKey);
    final byte[] lua =
        ("local val = redis.call('HGET', KEYS[1], ARGV[1]) "
                + "if val then redis.call('HSET', KEYS[1], ARGV[1], ARGV[2]) end "
                + "return val")
            .getBytes();

    RedisSerializer<String> keySer = (RedisSerializer<String>) redisTemplate.getStringSerializer();
    byte[] keyBytes = keySer.serialize(redisKey);
    byte[] hashKeyBytes = keySer.serialize(hashKey);
    byte[] resetBytes = keySer.serialize(resetValue);

    try {
      Object result =
          redisTemplate.execute(
              (RedisCallback<Object>)
                  connection ->
                      connection.eval(
                          lua, ReturnType.VALUE, 1, keyBytes, hashKeyBytes, resetBytes));
      if (result instanceof byte[]) {
        return new String((byte[]) result);
      }
      return result != null ? result.toString() : null;
    } catch (Exception ex) {
      log.error(
          "Lua getAndResetHash failed for key={}, field={}: {}",
          cacheKey,
          hashKey,
          ex.getMessage());
      return null;
    }
  }

  // --- GENERAL OPERATIONS ---

  public void evict(String cacheKey) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    redisTemplate.delete(buildKey(cacheKey));
  }

  public boolean expire(String cacheKey, Duration ttl) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    if (ttl == null) return false;
    return Boolean.TRUE.equals(redisTemplate.expire(buildKey(cacheKey), ttl));
  }

  public boolean evictIfValueMatches(String cacheKey, Object expectedValue) {
    Assert.hasText(cacheKey, "cacheKey must not be blank");
    if (expectedValue == null) return false;

    final String redisKey = buildKey(cacheKey);
    RedisSerializer<String> keySer = (RedisSerializer<String>) redisTemplate.getStringSerializer();
    RedisSerializer<Object> valueSer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();

    byte[] keyBytes = keySer.serialize(redisKey);
    byte[] valBytes = valueSer.serialize(expectedValue);

    // Script Lua: Nếu GET(key) == token thì DEL(key) else return 0
    final byte[] lua =
        ("if redis.call('get', KEYS[1]) == ARGV[1] then "
                + "return redis.call('del', KEYS[1]) "
                + "else return 0 end")
            .getBytes();

    try {
      Object result =
          redisTemplate.execute(
              (RedisCallback<Object>)
                  connection -> connection.eval(lua, ReturnType.INTEGER, 1, keyBytes, valBytes));

      if (result instanceof Long) return (Long) result == 1L;
      if (result instanceof Integer) return (Integer) result == 1;
      return false;
    } catch (Exception ex) {
      log.error("Lua evict failed for key {}: {}", cacheKey, ex.getMessage());
      return false;
    }
  }

  private String buildKey(String cacheKey) {
    String prefix = hotCacheProperties.getKeyPrefix();
    return (prefix != null && !prefix.isBlank()) ? prefix + ":" + cacheKey : cacheKey;
  }
}
