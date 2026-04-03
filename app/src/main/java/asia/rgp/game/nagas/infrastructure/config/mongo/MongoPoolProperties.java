package asia.rgp.game.nagas.infrastructure.config.mongo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Encapsulates MongoDB connection pool tuning parameters. */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "mongo.pool")
public class MongoPoolProperties {

  @Min(0)
  private int minSize = 0;

  @Min(1)
  private int maxSize = 50;

  @NotNull private Duration maxConnectionIdleTime = Duration.ofMinutes(1);

  @NotNull private Duration maxConnectionLifeTime = Duration.ofMinutes(5);

  @NotNull private Duration maxWaitTime = Duration.ofSeconds(5);

  @Min(1)
  @Max(120)
  private int maintenanceFrequencySeconds = 60;
}
