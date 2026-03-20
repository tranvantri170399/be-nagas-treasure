package asia.rgp.game.nagas.infrastructure.config.redis;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Properties for application-level hot cache stored in Redis.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "redis.cache")
public class HotCacheProperties {

    @NotNull
    private Duration defaultTtl = Duration.ofMinutes(5);

    @NotBlank
    private String keyPrefix = "game-nags:cache";
}

