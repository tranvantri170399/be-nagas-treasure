package asia.rgp.game.nagas.infrastructure.config.redis;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Redis connection properties bound to the standard Spring Boot prefix: "spring.redis".
 *
 * Note: application.yml must use keys like spring.redis.host, spring.redis.port, ...
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "spring.redis")
public class RedisConnectionProperties {

    /**
     * Redis host (hostname or IP).
     */
    @NotBlank
    private String host = "localhost";

    /**
     * Redis TCP port.
     */
    @Min(1)
    private int port = 6379;

    /**
     * Redis database index.
     */
    private int database = 0;

    /**
     * Redis password (optional).
     */
    private String password;

    /**
     * Command timeout for Lettuce (ISO-8601 duration). Default: PT2S (2 seconds).
     */
    private Duration timeout = Duration.ofSeconds(2);

    /**
     * Whether to use TLS/SSL for Redis connection.
     */
    private boolean ssl = false;
}
