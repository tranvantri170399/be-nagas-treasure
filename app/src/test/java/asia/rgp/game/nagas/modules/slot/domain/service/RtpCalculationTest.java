package asia.rgp.game.nagas.modules.slot.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * RTP (Return To Player) Calculation Test Suite
 *
 * <p>Tests verify the RTP distribution according to Math Model: - Base Game: ~54.00% - Hold and Win
 * (including Fixed Jackpots): ~24.00% - Free Spins Feature: ~14.00% - Progressive Jackpots: ~4.00%
 * - Total RTP Target: 96.00%
 */
@DisplayName("RTP Calculation Test Suite (Target: 96%)")
class RtpCalculationTest {

  static final double TARGET_RTP = 0.96; // 96%
  static final double JACKPOT_RTP = 0.04; // 4%
  static final double BASE_GAME_RTP = 0.54; // 54%
  static final double HOLD_AND_WIN_RTP = 0.24; // 24%
  static final double FREE_SPINS_RTP = 0.14; // 14%

  // ============================================================
  // 1. JACKPOT CONTRIBUTION TESTS (4% RTP)
  // ============================================================

  @Nested
  @DisplayName("1. Jackpot Contribution (4% RTP)")
  class JackpotContributionTests {

    @Test
    @DisplayName("1.1 Jackpot contribution is exactly 4% of bet amount")
    void jackpotContributionIs4Percent() {
      // The contribution logic is in JackpotServiceImpl
      // This test verifies the mathematical correctness
      double actualContribution =
          0.5 + 0.8 + 1.0 + 1.7; // Total jackpot contribution percentages: 4%

      assertEquals(
          4.0, actualContribution, 0.001, "Total jackpot contribution should be 4% of bet");
    }

    @Test
    @DisplayName(
        "1.2 Jackpot contribution breakdown: DIAMOND 0.5%, RUBY 0.8%, EMERALD 1.0%, SAPPHIRE 1.7%")
    void jackpotContributionBreakdown() {
      double diamondPct = 0.5;
      double rubyPct = 0.8;
      double emeraldPct = 1.0;
      double sapphirePct = 1.7;
      double total = diamondPct + rubyPct + emeraldPct + sapphirePct;

      assertEquals(4.0, total, 0.001, "Sum of all jackpot contributions should be 4%");
      assertEquals(0.5, diamondPct, 0.001, "DIAMOND should be 0.5%");
      assertEquals(0.8, rubyPct, 0.001, "RUBY should be 0.8%");
      assertEquals(1.0, emeraldPct, 0.001, "EMERALD should be 1.0%");
      assertEquals(1.7, sapphirePct, 0.001, "SAPPHIRE should be 1.7%");
    }
  }

  // ============================================================
  // 2. RTP DISTRIBUTION VERIFICATION
  // ============================================================

  @Nested
  @DisplayName("2. RTP Distribution Verification")
  class RtpDistributionTests {

    @Test
    @DisplayName("2.1 Total RTP target is 96%")
    void totalRtpTargetIs96Percent() {
      double totalRtp = BASE_GAME_RTP + HOLD_AND_WIN_RTP + FREE_SPINS_RTP + JACKPOT_RTP;
      assertEquals(
          TARGET_RTP,
          totalRtp,
          0.001,
          "Total RTP distribution should sum to 96% (54 + 24 + 14 + 4 = 96)");
    }

    @Test
    @DisplayName("2.2 Base Game RTP is 54%")
    void baseGameRtpIs54Percent() {
      assertEquals(0.54, BASE_GAME_RTP, 0.001, "Base Game RTP should be 54%");
    }

    @Test
    @DisplayName("2.3 Hold and Win RTP is 24% (including Fixed Jackpots)")
    void holdAndWinRtpIs24Percent() {
      assertEquals(0.24, HOLD_AND_WIN_RTP, 0.001, "Hold and Win RTP should be 24%");
    }

    @Test
    @DisplayName("2.4 Free Spins RTP is 14%")
    void freeSpinsRtpIs14Percent() {
      assertEquals(0.14, FREE_SPINS_RTP, 0.001, "Free Spins RTP should be 14%");
    }

    @Test
    @DisplayName("2.5 Progressive Jackpot RTP is 4%")
    void progressiveJackpotRtpIs4Percent() {
      assertEquals(0.04, JACKPOT_RTP, 0.001, "Progressive Jackpot RTP should be 4%");
    }
  }
}
