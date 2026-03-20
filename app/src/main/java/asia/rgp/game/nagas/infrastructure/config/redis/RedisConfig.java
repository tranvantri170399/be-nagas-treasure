package asia.rgp.game.nagas.infrastructure.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Configures Redis connection factory and templates for caching.
 * Final version: Clean build with zero warnings.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({HotCacheProperties.class, RedisConnectionProperties.class})
public class RedisConfig {

    private final HotCacheProperties hotCacheProperties;
    private final RedisConnectionProperties redisConnectionProperties;
    private final ObjectMapper objectMapper;

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
        
        boolean useSsl = redisConnectionProperties.isSsl();
        if (useSsl) {
            clientConfigBuilder.useSsl();
        }

        log.info("Connecting to Redis at {}:{}, db={}, ssl={}",
                redisConnectionProperties.getHost(), redisConnectionProperties.getPort(),
                redisConnectionProperties.getDatabase(), useSsl);

        return new LettuceConnectionFactory(standaloneConfiguration, clientConfigBuilder.build());
    }

    /**
     * Configures the RedisTemplate with modern serializers.
     * Uses RedisSerializer.json() to avoid [removal] warnings.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}