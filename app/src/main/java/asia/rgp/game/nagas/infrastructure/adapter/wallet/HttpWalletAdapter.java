package asia.rgp.game.nagas.infrastructure.adapter.wallet;

import asia.rgp.game.nagas.infrastructure.http.BaseRestClientAdapter;
import asia.rgp.game.nagas.modules.slot.application.port.out.WalletPort;
import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.domain.model.Money;
import asia.rgp.game.nagas.shared.error.ErrorCode;
import asia.rgp.game.nagas.shared.infrastructure.exception.ExternalServiceException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Primary
@Component
@ConditionalOnProperty(prefix = "wallet.http", name = "enabled", havingValue = "true")
public class HttpWalletAdapter extends BaseRestClientAdapter implements WalletPort {

  private static final String TRANSFER_PATH = "/transfer";

  private final WalletHttpProperties properties;

  public HttpWalletAdapter(RestTemplate restTemplate, WalletHttpProperties properties) {
    super(restTemplate, "SeamlessWallet");
    this.properties = properties;
  }

  @Override
  public long getBalance(String agentId, String userId) {
    try {
      TransferResultData result =
          transfer(agentId, userId, Action.LOSE, 0L, UUID.randomUUID().toString());
      return resolveBalance(result);
    } catch (DomainException e) {
      throw e;
    } catch (ExternalServiceException e) {
      throw new DomainException(
          "Failed to fetch balance from wallet service", ErrorCode.BALANCE_SERVICE_UNAVAILABLE, e);
    }
  }

  @Override
  public void debit(String agentId, String userId, Money amount, String transactionId) {
    long amountInCents = Math.round(amount.getAmount() * 100.0);
    try {
      transfer(agentId, userId, Action.BET, amountInCents, transactionId);
    } catch (DomainException e) {
      throw e;
    } catch (ExternalServiceException e) {
      throw new DomainException(
          "Wallet debit request failed", ErrorCode.BALANCE_SERVICE_UNAVAILABLE, e);
    }
  }

  @Override
  public void credit(String agentId, String userId, Money amount, String transactionId) {
    long amountInCents = Math.round(amount.getAmount() * 100.0);
    try {
      transfer(agentId, userId, Action.WIN, amountInCents, transactionId);
    } catch (DomainException e) {
      throw e;
    } catch (ExternalServiceException e) {
      throw new DomainException(
          "Wallet credit request failed", ErrorCode.BALANCE_SERVICE_UNAVAILABLE, e);
    }
  }

  private TransferResultData transfer(
      String agentId, String userId, Action action, long amountInCents, String transactionId) {
    TransferRequest request =
        buildTransferRequest(agentId, userId, action, amountInCents, transactionId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String url = normalizeBaseUrl(properties.getBaseUrl()) + TRANSFER_PATH;

    TransferResponse response =
        executePostRequest(
            url,
            request,
            TransferResponse.class,
            body -> body,
            "wallet transfer",
            ErrorCode.BALANCE_SERVICE_ERROR,
            ErrorCode.BALANCE_SERVICE_UNAVAILABLE,
            headers);

    if (response == null) {
      throw new DomainException("Wallet response is empty", ErrorCode.BALANCE_OPERATION_FAILED);
    }

    if (!"OK".equalsIgnoreCase(response.getStatus())) {
      throw new DomainException(
          "Wallet transfer failed: " + safeMessage(response.getMessage()),
          ErrorCode.BALANCE_OPERATION_FAILED);
    }

    if (response.getData() == null || response.getData().isEmpty()) {
      throw new DomainException("Wallet response has no data", ErrorCode.BALANCE_OPERATION_FAILED);
    }

    TransferResultData item = response.getData().get(0);
    String itemStatus = item.getStatus() == null ? "" : item.getStatus();
    int errorCode = item.getErrorCode() == null ? 500 : item.getErrorCode();

    if (!"OK".equalsIgnoreCase(itemStatus) || errorCode != 200) {
      String message = safeMessage(item.getMessage());
      ErrorCode mapped = mapWalletErrorCode(message);
      throw new DomainException("Wallet transfer rejected: " + message, mapped);
    }

    return item;
  }

  private TransferRequest buildTransferRequest(
      String agentId, String userId, Action action, long amountInCents, String transactionId) {
    int agencyId = resolveAgencyId(agentId);

    TransferGameData data = new TransferGameData();
    data.setGameId(properties.getGameId());
    data.setGameName(properties.getGameName());
    data.setGameRoundId(transactionId);
    data.setGameTicketId(transactionId);
    data.setGameTicketStatus("Running");
    data.setGameYourBet(action.name());
    data.setGameStake(action == Action.BET ? amountInCents : 0L);
    data.setGameWinlost(
        action == Action.WIN ? amountInCents : (action == Action.BET ? -amountInCents : 0L));
    data.setGameGain(action == Action.WIN ? amountInCents : 0L);
    data.setGameTax(0L);
    data.setGameRefund(0L);
    data.setGameBetValue(action == Action.BET ? amountInCents : 0L);
    data.setGameReserve(0L);
    data.setPlatformId(properties.getPlatformId());
    data.setIpPlay(properties.getDefaultIpPlay());

    TransferItem item = new TransferItem();
    item.setToken(null);
    item.setUid(userId);
    item.setAgencyId(agencyId);
    item.setMemberId(resolveMemberId(userId));
    item.setAmount(amountInCents);
    item.setTransactionId(transactionId);
    item.setAction(action.name());
    item.setData(data);

    TransferRequest request = new TransferRequest();
    request.setAgencyId(agencyId);
    request.setTransfers(List.of(item));
    return request;
  }

  private long resolveBalance(TransferResultData result) {
    if (result == null) {
      return 0L;
    }
    if (result.getAmountAfter() != null) {
      return result.getAmountAfter();
    }
    if (result.getAmount() != null) {
      return result.getAmount();
    }
    if (result.getWallets() != null) {
      return result.getWallets().stream()
          .filter(wallet -> wallet.getType() != null && wallet.getType() == 99)
          .map(WalletBalance::getBalance)
          .filter(balance -> balance != null)
          .findFirst()
          .orElse(0L);
    }
    return 0L;
  }

  private int resolveAgencyId(String agentId) {
    if (agentId == null || agentId.isBlank()) {
      return properties.getDefaultAgencyId();
    }

    String digits = agentId.replaceAll("[^0-9]", "");
    if (digits.isBlank()) {
      return properties.getDefaultAgencyId();
    }

    try {
      return Integer.parseInt(digits);
    } catch (NumberFormatException ignored) {
      return properties.getDefaultAgencyId();
    }
  }

  private Long resolveMemberId(String userId) {
    if (userId == null || userId.isBlank()) {
      return null;
    }

    String digits = userId.replaceAll("[^0-9]", "");
    if (digits.isBlank()) {
      return null;
    }

    try {
      return Long.parseLong(digits);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private ErrorCode mapWalletErrorCode(String message) {
    String msg = message == null ? "" : message.toLowerCase(Locale.ROOT);
    if (msg.contains("insufficient") || msg.contains("not enough")) {
      return ErrorCode.INSUFFICIENT_BALANCE;
    }
    return ErrorCode.BALANCE_OPERATION_FAILED;
  }

  private String normalizeBaseUrl(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new DomainException("wallet.http.base-url is missing", ErrorCode.BALANCE_SERVICE_ERROR);
    }
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  private String safeMessage(String message) {
    return message == null || message.isBlank() ? "Unknown wallet error" : message;
  }

  private enum Action {
    BET,
    WIN,
    LOSE
  }

  @Data
  private static class TransferRequest {
    @JsonProperty("agency_id")
    private Integer agencyId;

    @JsonProperty("transfers")
    private List<TransferItem> transfers;
  }

  @Data
  private static class TransferItem {
    @JsonProperty("token")
    private String token;

    @JsonProperty("uid")
    private String uid;

    @JsonProperty("agency_id")
    private Integer agencyId;

    @JsonProperty("member_id")
    private Long memberId;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("data")
    private TransferGameData data;
  }

  @Data
  private static class TransferGameData {
    @JsonProperty("game_id")
    private String gameId;

    @JsonProperty("game_name")
    private String gameName;

    @JsonProperty("game_round_id")
    private String gameRoundId;

    @JsonProperty("game_ticket_id")
    private String gameTicketId;

    @JsonProperty("game_ticket_status")
    private String gameTicketStatus;

    @JsonProperty("game_your_bet")
    private String gameYourBet;

    @JsonProperty("game_stake")
    private Long gameStake;

    @JsonProperty("game_winlost")
    private Long gameWinlost;

    @JsonProperty("game_gain")
    private Long gameGain;

    @JsonProperty("game_tax")
    private Long gameTax;

    @JsonProperty("game_refund")
    private Long gameRefund;

    @JsonProperty("game_bet_value")
    private Long gameBetValue;

    @JsonProperty("game_reserve")
    private Long gameReserve;

    @JsonProperty("platform_id")
    private Integer platformId;

    @JsonProperty("ip_play")
    private String ipPlay;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class TransferResponse {
    @JsonProperty("status")
    private String status;

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("data")
    private List<TransferResultData> data;

    @JsonProperty("message")
    private String message;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class TransferResultData {
    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("amount_before")
    private Long amountBefore;

    @JsonProperty("amount_after")
    private Long amountAfter;

    @JsonProperty("req_amount")
    private Long reqAmount;

    @JsonProperty("dues_amount")
    private Long duesAmount;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("agency_transaction_id")
    private String agencyTransactionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error_code")
    private Integer errorCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("time")
    private Long time;

    @JsonProperty("wallets")
    private List<WalletBalance> wallets;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class WalletBalance {
    @JsonProperty("type")
    private Integer type;

    @JsonProperty("balance")
    private Long balance;
  }
}
