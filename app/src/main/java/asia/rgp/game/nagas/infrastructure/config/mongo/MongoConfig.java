package asia.rgp.game.nagas.infrastructure.config.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.UuidRepresentation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.Assert;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({MongoPoolProperties.class, AppMongoProperties.class})
public class MongoConfig {

  private final MongoPoolProperties poolProperties;
  private final AppMongoProperties appMongoProperties;

  @Bean
  public MongoClient mongoClient() {
    String uri = appMongoProperties.getUri();
    Assert.hasText(uri, "MongoDB connection URI must be provided (spring.data.mongodb.uri)");

    ConnectionString connectionString = new ConnectionString(uri);

    MongoClientSettings settings =
        MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .applyToConnectionPoolSettings(
                builder ->
                    builder
                        .minSize(poolProperties.getMinSize())
                        .maxSize(poolProperties.getMaxSize())
                        .maxConnectionIdleTime(
                            poolProperties.getMaxConnectionIdleTime().toMillis(),
                            TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(
                            poolProperties.getMaxConnectionLifeTime().toMillis(),
                            TimeUnit.MILLISECONDS)
                        .maxWaitTime(
                            poolProperties.getMaxWaitTime().toMillis(), TimeUnit.MILLISECONDS)
                        .maintenanceFrequency(
                            poolProperties.getMaintenanceFrequencySeconds(), TimeUnit.SECONDS))
            .build();

    log.info(
        "MongoDB client configured with pool size {} - {} and URI {}",
        poolProperties.getMinSize(),
        poolProperties.getMaxSize(),
        sanitizeUri(uri));

    return MongoClients.create(settings);
  }

  @Bean
  public MongoTemplate mongoTemplate(MongoClient mongoClient) {
    String database = appMongoProperties.getDatabase();

    if (database == null || database.isBlank()) {
      database = new ConnectionString(appMongoProperties.getUri()).getDatabase();
    }
    Assert.hasText(
        database, "MongoDB database name must be provided (spring.data.mongodb.database)");

    return new MongoTemplate(mongoClient, database);
  }

  private String sanitizeUri(String uri) {
    int idx = uri.indexOf('@');
    if (idx > -1) {
      return "mongodb://***:***" + uri.substring(idx);
    }
    return uri;
  }
}
