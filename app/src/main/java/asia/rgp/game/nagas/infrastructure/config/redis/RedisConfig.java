package asia.rgp.game.nagas.infrastructure.config.redis;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

/**
 * Configures Redis connection factory and templates for caching. Fixed version: Supports atomic
 * numeric operations for Jackpot pools.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({HotCacheProperties.class, RedisConnectionProperties.class})
public class RedisConfig {

  private final RedisConnectionProperties redisConnectionProperties;

  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
    standaloneConfiguration.setHostName(redisConnectionProperties.getHost());
    standaloneConfiguration.setPort(redisConnectionProperties.getPort());
    standaloneConfiguration.setDatabase(redisConnectionProperties.getDatabase());

    if (StringUtils.hasText(redisConnectionProperties.getPassword())) {
      standaloneConfiguration.setPassword(redisConnectionProperties.getPassword());
    }

    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
        LettuceClientConfiguration.builder();

    Duration timeout = redisConnectionProperties.getTimeout();
    if (timeout != null) {
      clientConfigBuilder.commandTimeout(timeout);
    }

    if (redisConnectionProperties.isSsl()) {
      clientConfigBuilder.useSsl();
    }

    log.info(
        "Connecting to Redis at {}:{}, db={}, ssl={}",
        redisConnectionProperties.getHost(),
        redisConnectionProperties.getPort(),
        redisConnectionProperties.getDatabase(),
        redisConnectionProperties.isSsl());

    return new LettuceConnectionFactory(standaloneConfiguration, clientConfigBuilder.build());
  }

  /**
   * Configures the RedisTemplate with specific serializers. CRITICAL: HashValueSerializer must be
   * StringRedisSerializer to support HINCRBYFLOAT.
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    StringRedisSerializer stringSerializer = new StringRedisSerializer();

    RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();

    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);

    template.setHashValueSerializer(stringSerializer);

    template.setValueSerializer(jsonSerializer);

    template.afterPropertiesSet();
    return template;
  }
}
