package asia.rgp.game.nagas.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import asia.rgp.game.nagas.infrastructure.config.redis.HotCacheProperties;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "deprecation"})
public class HotCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HotCacheProperties hotCacheProperties;

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
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(buildKey(cacheKey), value, effectiveTtl);
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

    public void evict(String cacheKey) {
        Assert.hasText(cacheKey, "cacheKey must not be blank");
        redisTemplate.delete(buildKey(cacheKey));
    }

    public boolean evictIfValueMatches(String cacheKey, Object expectedValue) {
        Assert.hasText(cacheKey, "cacheKey must not be blank");
        if (expectedValue == null) return false;

        final String redisKey = buildKey(cacheKey);
        RedisSerializer<String> keySer = (RedisSerializer<String>) redisTemplate.getStringSerializer();
        RedisSerializer<Object> valueSer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();

        byte[] keyBytes = keySer.serialize(redisKey);
        byte[] valBytes = valueSer.serialize(expectedValue);

        final byte[] lua = ("if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) " +
                "else return 0 end").getBytes();

        try {
            Object result = redisTemplate.execute((RedisCallback<Object>) connection -> 
                connection.eval(lua, ReturnType.INTEGER, 1, keyBytes, valBytes));
            
            if (result instanceof Long) return (Long) result == 1L;
            if (result instanceof Integer) return (Integer) result == 1;
            return false;
        } catch (Exception ex) {
            log.error("Lua evict failed for key {}: {}", cacheKey, ex.getMessage());
            return false;
        }
    }

    public long increment(String cacheKey) {
        Assert.hasText(cacheKey, "cacheKey must not be blank");
        String redisKey = buildKey(cacheKey);
        Long result = redisTemplate.opsForValue().increment(redisKey);
        return result != null ? result : 0L;
    }

    public boolean expire(String cacheKey, Duration ttl) {
        Assert.hasText(cacheKey, "cacheKey must not be blank");
        if (ttl == null) return false;
        String redisKey = buildKey(cacheKey);
        Boolean result = redisTemplate.expire(redisKey, ttl);
        return Boolean.TRUE.equals(result);
    }
    
    private String buildKey(String cacheKey) {
        String prefix = hotCacheProperties.getKeyPrefix();
        return prefix + ":" + cacheKey;
    }
}