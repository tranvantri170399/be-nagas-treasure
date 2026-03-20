package asia.rgp.game.nagas.shared.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Session information stored in Redis.
 * Key format: session:{sessionToken}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionInfo {

    @JsonProperty("createdAt")
    private Long createdAt;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("gameCode")
    private String gameCode;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("playerId")
    private String playerId;

    @JsonProperty("username")
    private String username;
}
