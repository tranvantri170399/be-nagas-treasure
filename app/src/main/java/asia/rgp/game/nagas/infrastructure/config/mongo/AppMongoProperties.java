package asia.rgp.game.nagas.infrastructure.config.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Custom MongoDB properties, decoupled from Spring Boot autoconfigure classes.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class AppMongoProperties {

    /**
     * MongoDB connection URI.
     */
    private String uri;

    /**
     * MongoDB database name (optional, can be part of URI).
     */
    private String database;
}


