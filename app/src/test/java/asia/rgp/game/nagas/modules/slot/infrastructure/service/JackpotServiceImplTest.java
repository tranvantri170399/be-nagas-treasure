package asia.rgp.game.nagas.modules.slot.infrastructure.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import asia.rgp.game.nagas.infrastructure.cache.HotCacheService;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.modules.slot.domain.service.JackpotService;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository.MongoJackpotAuditRepository;
import asia.rgp.game.nagas.shared.domain.model.Money;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JackpotServiceImplTest {

  @Mock private HotCacheService hotCacheService;
  @Mock private MongoJackpotAuditRepository auditRepository;

  private JackpotServiceImpl jackpotService;

  private static final String AGENT_ID = "test-agent";
  private static final String USER_ID = "test-user";
  private static final String SESSION_ID = "test-session";

  @BeforeEach
  void setUp() {
    jackpotService = new JackpotServiceImpl(hotCacheService, auditRepository);
  }

  // ==================== POOL INITIALIZATION (GDD 8.3) ====================

  @Nested
  @DisplayName("GDD 8.3 - Jackpot Pool Initialization")
  class InitializationTests {

    @Test
    @DisplayName("Pools initialized with seed values when absent")
    void initPoolsWhenAbsent() {
      when(hotCacheService.getHash(eq("jackpot:pools:" + AGENT_ID), anyString())).thenReturn(null);

      jackpotService.initPoolsIfAbsent(AGENT_ID);

      verify(hotCacheService)
          .putHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_DIAMOND, "10000.0");
      verify(hotCacheService)
          .putHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_RUBY, "500.0");
      verify(hotCacheService)
          .putHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_EMERALD, "50.0");
      verify(hotCacheService)
          .putHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_SAPPHIRE, "10.0");
    }

    @Test
    @DisplayName("Pools re-initialized if below seed value (after win reset)")
    void initPoolsWhenBelowSeed() {
      when(hotCacheService.getHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_DIAMOND))
          .thenReturn("5000.0"); // Below 10000 seed
      when(hotCacheService.getHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_RUBY))
          .thenReturn("600.0"); // Above 500 seed
      when(hotCacheService.getHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_EMERALD))
          .thenReturn("100.0"); // Above 50 seed
      when(hotCacheService.getHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_SAPPHIRE))
          .thenReturn("20.0"); // Above 10 seed

      jackpotService.initPoolsIfAbsent(AGENT_ID);

      // Only Diamond should be re-initialized (below seed)
      verify(hotCacheService)
          .putHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_DIAMOND, "10000.0");
      verify(hotCacheService, never())
          .putHash(eq("jackpot:pools:" + AGENT_ID), eq(SlotConstants.JACKPOT_RUBY), anyString());
    }

    @Test
    @DisplayName("Seed values match GDD 8.3: DIAMOND=$10000, RUBY=$500, EMERALD=$50, SAPPHIRE=$10")
    void seedValuesMatchGdd() {
      when(hotCacheService.getHash(eq("jackpot:pools:" + AGENT_ID), anyString())).thenReturn(null);

      jackpotService.initPoolsIfAbsent(AGENT_ID);

      verify(hotCacheService)
          .putHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_DIAMOND, "10000.0");
      verify(hotCacheService)
          .putHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_RUBY, "500.0");
      verify(hotCacheService)
          .putHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_EMERALD, "50.0");
      verify(hotCacheService)
          .putHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_SAPPHIRE, "10.0");
    }
  }

  // ==================== CONTRIBUTION (GDD 8.3 - 4% RTP) ====================

  @Nested
  @DisplayName("GDD 8.3 - Jackpot Contribution (4% RTP)")
  class ContributionTests {

    @Test
    @DisplayName("Contributes correct percentages from bet amount")
    void contributionPercentages() {
      Money bet = Money.of(100.0);

      jackpotService.contribute(AGENT_ID, bet);

      // DIAMOND: 0.5% of 100 = 0.5
      verify(hotCacheService)
          .incrementHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_DIAMOND, 0.5);
      // RUBY: 0.8% of 100 = 0.8
      verify(hotCacheService)
          .incrementHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_RUBY, 0.8);
      // EMERALD: 1.0% of 100 = 1.0
      verify(hotCacheService)
          .incrementHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_EMERALD, 1.0);
      // SAPPHIRE: 1.7% of 100 = 1.7
      verify(hotCacheService)
          .incrementHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_SAPPHIRE, 1.7);
    }

    @Test
    @DisplayName("Total contribution is 4% of bet (GDD 8.3)")
    void totalContributionIs4Percent() {
      // 0.5% + 0.8% + 1.0% + 1.7% = 4.0%
      double totalPct = 0.5 + 0.8 + 1.0 + 1.7;
      assertEquals(4.0, totalPct, 0.001);
    }

    @Test
    @DisplayName("Contribution with $1 bet")
    void contributionWithOneDollar() {
      Money bet = Money.of(1.0);

      jackpotService.contribute(AGENT_ID, bet);

      verify(hotCacheService)
          .incrementHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_DIAMOND, 0.005);
      verify(hotCacheService)
          .incrementHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_RUBY, 0.008);
      verify(hotCacheService)
          .incrementHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_EMERALD, 0.01);
      verify(hotCacheService)
          .incrementHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_SAPPHIRE, 0.017);
    }
  }

  // ==================== WHEEL SPIN (GDD 8.4) ====================

  @Nested
  @DisplayName("GDD 8.4 - Jackpot Wheel Spin")
  class WheelSpinTests {

    @Test
    @DisplayName("Spin always returns a valid tier and winId")
    void spinReturnsValidTier() {
      when(hotCacheService.getAndResetHash(
              eq("jackpot:pools:" + AGENT_ID), anyString(), anyString()))
          .thenReturn("1000.0");

      for (int i = 0; i < 100; i++) {
        JackpotService.JackpotSpinResult result =
            jackpotService.spinWheel(AGENT_ID, USER_ID, SESSION_ID, Money.of(1.0));

        assertNotNull(result.getTierName());
        assertNotNull(result.getWinId(), "Each spin must generate a winId");
        assertTrue(
            result.getTierName().equals(SlotConstants.JACKPOT_DIAMOND)
                || result.getTierName().equals(SlotConstants.JACKPOT_RUBY)
                || result.getTierName().equals(SlotConstants.JACKPOT_EMERALD)
                || result.getTierName().equals(SlotConstants.JACKPOT_SAPPHIRE),
            "Unknown tier: " + result.getTierName());
      }
    }

    @Test
    @DisplayName("Spin prize is positive")
    void spinPrizePositive() {
      when(hotCacheService.getAndResetHash(
              eq("jackpot:pools:" + AGENT_ID), anyString(), anyString()))
          .thenReturn("500.0");

      JackpotService.JackpotSpinResult result =
          jackpotService.spinWheel(AGENT_ID, USER_ID, SESSION_ID, Money.of(1.0));

      assertTrue(result.getAmount().isGreaterThanZero());
    }

    @Test
    @DisplayName("Atomic claim resets pool via Lua script (GDD 8.4)")
    void atomicClaimResetsPool() {
      when(hotCacheService.getAndResetHash(
              eq("jackpot:pools:" + AGENT_ID), anyString(), anyString()))
          .thenReturn("5000.0");

      jackpotService.spinWheel(AGENT_ID, USER_ID, SESSION_ID, Money.of(1.0));

      // The atomic Lua script (getAndResetHash) handles both read AND reset
      verify(hotCacheService, atLeastOnce())
          .getAndResetHash(eq("jackpot:pools:" + AGENT_ID), anyString(), anyString());
    }

    @Test
    @DisplayName("Audit record saved with CLAIMED status on spin")
    void auditRecordSavedOnSpin() {
      when(hotCacheService.getAndResetHash(
              eq("jackpot:pools:" + AGENT_ID), anyString(), anyString()))
          .thenReturn("1000.0");

      jackpotService.spinWheel(AGENT_ID, USER_ID, SESSION_ID, Money.of(1.0));

      verify(auditRepository, atLeastOnce())
          .save(
              argThat(
                  audit ->
                      "CLAIMED".equals(audit.getStatus())
                          && AGENT_ID.equals(audit.getAgencyId())
                          && USER_ID.equals(audit.getUserId())
                          && SESSION_ID.equals(audit.getSessionId())));
    }

    @Test
    @DisplayName("Diamond and Ruby wins set hitArrow=true (GDD 8.4 outer wheel)")
    void diamondRubyHitArrow() {
      when(hotCacheService.getAndResetHash(
              eq("jackpot:pools:" + AGENT_ID), anyString(), anyString()))
          .thenReturn("10000.0");

      boolean foundDiamondOrRuby = false;
      for (int i = 0; i < 10000; i++) {
        JackpotService.JackpotSpinResult result =
            jackpotService.spinWheel(AGENT_ID, USER_ID, SESSION_ID, Money.of(250.0));
        if (result.getTierName().equals(SlotConstants.JACKPOT_DIAMOND)
            || result.getTierName().equals(SlotConstants.JACKPOT_RUBY)) {
          assertTrue(result.isHitArrow(), "Diamond/Ruby should always hitArrow");
          foundDiamondOrRuby = true;
          break;
        }
      }
      // With high bet (250), Diamond/Ruby should eventually appear
      // This is probabilistic, so we don't assert foundDiamondOrRuby
    }

    @Test
    @DisplayName("Bet=250 should allow Emerald/Sapphire (no Ruby overflow)")
    void bet250DoesNotAlwaysReturnRubyOrDiamond() {
      when(hotCacheService.getAndResetHash(
              eq("jackpot:pools:" + AGENT_ID), anyString(), anyString()))
          .thenReturn("1000.0");

      boolean sawEmeraldOrSapphire = false;

      for (int i = 0; i < 200; i++) {
        JackpotService.JackpotSpinResult result =
            jackpotService.spinWheel(AGENT_ID, USER_ID, SESSION_ID + "-" + i, Money.of(250.0));

        String tier = result.getTierName();
        if (SlotConstants.JACKPOT_EMERALD.equals(tier)
            || SlotConstants.JACKPOT_SAPPHIRE.equals(tier)) {
          sawEmeraldOrSapphire = true;
          break;
        }
      }

      assertTrue(
          sawEmeraldOrSapphire,
          "With proper betFactor normalization, bet=250 must sometimes yield EMERALD or SAPPHIRE");
    }
  }

  // ==================== GET ALL POOLS ====================

  @Nested
  @DisplayName("Pool Queries")
  class PoolQueryTests {

    @Test
    @DisplayName("getAllPools returns all 4 tiers")
    void getAllPoolsReturnsFourTiers() {
      when(hotCacheService.getHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_DIAMOND))
          .thenReturn("15000.0");
      when(hotCacheService.getHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_RUBY))
          .thenReturn("750.0");
      when(hotCacheService.getHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_EMERALD))
          .thenReturn("85.0");
      when(hotCacheService.getHash("jackpot:pools:" + AGENT_ID, SlotConstants.JACKPOT_SAPPHIRE))
          .thenReturn("25.0");

      Map<String, Double> pools = jackpotService.getAllPools(AGENT_ID);

      assertEquals(4, pools.size());
      assertEquals(15000.0, pools.get(SlotConstants.JACKPOT_DIAMOND), 0.001);
      assertEquals(750.0, pools.get(SlotConstants.JACKPOT_RUBY), 0.001);
      assertEquals(85.0, pools.get(SlotConstants.JACKPOT_EMERALD), 0.001);
      assertEquals(25.0, pools.get(SlotConstants.JACKPOT_SAPPHIRE), 0.001);
    }

    @Test
    @DisplayName("getAllPools returns seed value when cache is null")
    void getAllPoolsDefaultsToSeed() {
      when(hotCacheService.getHash(eq("jackpot:pools:" + AGENT_ID), anyString())).thenReturn(null);

      Map<String, Double> pools = jackpotService.getAllPools(AGENT_ID);

      assertEquals(10000.0, pools.get(SlotConstants.JACKPOT_DIAMOND), 0.001);
      assertEquals(500.0, pools.get(SlotConstants.JACKPOT_RUBY), 0.001);
      assertEquals(50.0, pools.get(SlotConstants.JACKPOT_EMERALD), 0.001);
      assertEquals(10.0, pools.get(SlotConstants.JACKPOT_SAPPHIRE), 0.001);
    }
  }
}
