package asia.rgp.game.nagas.infrastructure.adapter.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import asia.rgp.game.nagas.infrastructure.grpc.WalletRequestContext;
import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.domain.model.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class HttpWalletAdapterTest {

  private static final String BASE_URL = "https://wallet.test.local/wallet";
  private static final String TRANSFER_URL = BASE_URL + "/transfer";

  private RestTemplate restTemplate;
  private MockRestServiceServer mockServer;
  private HttpWalletAdapter adapter;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);

    WalletHttpProperties properties = new WalletHttpProperties();
    properties.setEnabled(true);
    properties.setBaseUrl(BASE_URL);
    properties.setDefaultAgencyId(1);
    properties.setPlatformId(1);
    properties.setGameId("nagas_treasure");
    properties.setGameName("Golden Nagas Treasure");
    properties.setDefaultIpPlay("127.0.0.1");
    properties.setAuthorization("Bearer static-test-token");

    adapter = new HttpWalletAdapter(restTemplate, properties);
  }

  @AfterEach
  void tearDown() {
    WalletRequestContext.clear();
  }

  // ---------------------------------------------------------------------------
  // getBalance
  // ---------------------------------------------------------------------------
  @Nested
  @DisplayName("getBalance")
  class GetBalance {

    @Test
    @DisplayName("should return amount_after from wallet response")
    void shouldReturnAmountAfter() {
      mockServer
          .expect(requestTo(TRANSFER_URL))
          .andExpect(method(HttpMethod.POST))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(content().json("{\"transfers\":[{\"action\":\"LOSE\",\"amount\":0}]}", false))
          .andRespond(
              withSuccess(
                  """
              {
                "status": "OK",
                "code": 200,
                "data": [{
                  "status": "OK",
                  "error_code": 200,
                  "amount_before": 500000,
                  "amount_after": 500000,
                  "transaction_id": "balance-001"
                }],
                "message": "Success"
              }""",
                  MediaType.APPLICATION_JSON));

      long balance = adapter.getBalance("1", "user-123");

      assertThat(balance).isEqualTo(500000L);
      mockServer.verify();
    }

    @Test
    @DisplayName("should fallback to wallets[type=99] when amount_after is null")
    void shouldFallbackToWalletType99() {
      mockServer
          .expect(requestTo(TRANSFER_URL))
          .andExpect(method(HttpMethod.POST))
          .andRespond(
              withSuccess(
                  """
              {
                "status": "OK",
                "code": 200,
                "data": [{
                  "status": "OK",
                  "error_code": 200,
                  "wallets": [
                    {"type": 99, "balance": 750000},
                    {"type": 101, "balance": 100}
                  ],
                  "transaction_id": "balance-002"
                }],
                "message": "Success"
              }""",
                  MediaType.APPLICATION_JSON));

      long balance = adapter.getBalance("1", "user-123");

      assertThat(balance).isEqualTo(750000L);
      mockServer.verify();
    }

    @Test
    @DisplayName("should throw when wallet response status is not OK")
    void shouldThrowWhenStatusNotOk() {
      mockServer
          .expect(requestTo(TRANSFER_URL))
          .andExpect(method(HttpMethod.POST))
          .andRespond(
              withSuccess(
                  """
              {
                "status": "ERROR",
                "code": 500,
                "data": null,
                "message": "Internal error"
              }""",
                  MediaType.APPLICATION_JSON));

      assertThatThrownBy(() -> adapter.getBalance("1", "user-123"))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("failed");
      mockServer.verify();
    }

    @Test
    @DisplayName("should throw when wallet data is empty")
    void shouldThrowWhenDataEmpty() {
      mockServer
          .expect(requestTo(TRANSFER_URL))
          .andExpect(method(HttpMethod.POST))
          .andRespond(
              withSuccess(
                  """
              {
                "status": "OK",
                "code": 200,
                "data": [],
                "message": "No data"
              }""",
                  MediaType.APPLICATION_JSON));

      assertThatThrownBy(() -> adapter.getBalance("1", "user-123"))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("no data");
      mockServer.verify();
    }

    @Test
    @DisplayName("should throw when wallet server returns 500")
    void shouldThrowOnServerError() {
      mockServer
          .expect(requestTo(TRANSFER_URL))
          .andExpect(method(HttpMethod.POST))
          .andRespond(withServerError());

      assertThatThrownBy(() -> adapter.getBalance("1", "user-123"))
          .isInstanceOf(DomainException.class);
      mockServer.verify();
    }
  }

  // ---------------------------------------------------------------------------
  // debit (BET)
  // ---------------------------------------------------------------------------
  @Nested
  @DisplayName("debit")
  class Debit {

    @Test
    @DisplayName("should send BET action with correct amount in cents")
    void shouldSendBetAction() {
      mockServer
          .expect(requestTo(TRANSFER_URL))
          .andExpect(method(HttpMethod.POST))
          .andExpect(
              content().json("{\"transfers\":[{\"action\":\"BET\",\"amount\":1000}]}", false))
          .andRespond(
              withSuccess(
                  okTransferResponse("OK", 200, 500000, 499000, "tx-bet-001"),
                  MediaType.APPLICATION_JSON));

      adapter.debit("1", "user-123", Money.of(10.0), "tx-bet-001");
      mockServer.verify();
    }

    @Test
    @DisplayName("should throw on insufficient balance")
    void shouldThrowOnInsufficientBalance() {
      mockServer
          .expect(requestTo(TRANSFER_URL))
          .andExpect(method(HttpMethod.POST))
          .andRespond(
              withSuccess(
                  """
              {
                "status": "OK",
                "code": 200,
                "data": [{
                  "status": "FAIL",
                  "error_code": 400,
                  "message": "Insufficient balance"
                }],
                "message": "OK"
              }""",
                  MediaType.APPLICATION_JSON));

      assertThatThrownBy(() -> adapter.debit("1", "user-123", Money.of(10.0), "tx-fail"))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("Insufficient balance");
      mockServer.verify();
    }
  }

  // ---------------------------------------------------------------------------
  // credit (WIN)
  // ---------------------------------------------------------------------------
  @Nested
  @DisplayName("credit")
  class Credit {

    @Test
    @DisplayName("should send WIN action with correct amount in cents")
    void shouldSendWinAction() {
      mockServer
          .expect(requestTo(TRANSFER_URL))
          .andExpect(method(HttpMethod.POST))
          .andExpect(
              content().json("{\"transfers\":[{\"action\":\"WIN\",\"amount\":3400}]}", false))
          .andRespond(
              withSuccess(
                  okTransferResponse("OK", 200, 499000, 502400, "tx-win-001"),
                  MediaType.APPLICATION_JSON));

      adapter.credit("1", "user-123", Money.of(34.0), "tx-win-001");
      mockServer.verify();
    }
  }

  // ---------------------------------------------------------------------------
  // Auth headers
  // ---------------------------------------------------------------------------
  @Nested
  @DisplayName("auth headers")
  class AuthHeaders {

    @Test
    @DisplayName("should use WalletRequestContext token when available")
    void shouldUseContextToken() {
      WalletRequestContext.set(
          WalletRequestContext.Context.builder()
              .token("user-session-token")
              .userId("user-123")
              .agency("1")
              .build());

      mockServer
          .expect(requestTo(TRANSFER_URL))
          .andExpect(method(HttpMethod.POST))
          .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer user-session-token"))
          .andRespond(
              withSuccess(
                  okTransferResponse("OK", 200, 500000, 500000, "auth-001"),
                  MediaType.APPLICATION_JSON));

      adapter.getBalance("1", "user-123");
      mockServer.verify();
    }

    @Test
    @DisplayName("should fallback to static authorization when no context token")
    void shouldFallbackToStaticAuth() {
      // No WalletRequestContext set — should use properties.authorization
      mockServer
          .expect(requestTo(TRANSFER_URL))
          .andExpect(method(HttpMethod.POST))
          .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer static-test-token"))
          .andRespond(
              withSuccess(
                  okTransferResponse("OK", 200, 500000, 500000, "auth-002"),
                  MediaType.APPLICATION_JSON));

      adapter.getBalance("1", "user-123");
      mockServer.verify();
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------
  private static String okTransferResponse(
      String itemStatus, int errorCode, long before, long after, String txId) {
    return String.format(
        """
        {
          "status": "OK",
          "code": 200,
          "data": [{
            "status": "%s",
            "error_code": %d,
            "amount_before": %d,
            "amount_after": %d,
            "transaction_id": "%s"
          }],
          "message": "Success"
        }""",
        itemStatus, errorCode, before, after, txId);
  }
}
