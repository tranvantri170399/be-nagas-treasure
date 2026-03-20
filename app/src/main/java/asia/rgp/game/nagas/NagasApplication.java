package asia.rgp.game.nagas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository")
public class NagasApplication {
    public static void main(String[] args) {
        SpringApplication.run(NagasApplication.class, args);
    }
}