package asia.rgp.game.nagas.infrastructure.adapter.wallet;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wallet.http")
public class WalletHttpProperties {

  private boolean enabled = false;
  private String baseUrl = "https://stag.osyamazakiglobel.club/wallet";
  private int defaultAgencyId = 1;
  private int platformId = 1;
  private String gameId = "nagas_treasure";
  private String gameName = "Golden Nagas Treasure";
  private String defaultIpPlay = "127.0.0.1";
  private String authorization = "";
  private String apiKeyHeader = "";
  private String apiKey = "";
}
