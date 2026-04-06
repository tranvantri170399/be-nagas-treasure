package asia.rgp.game.nagas.modules.slot.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SlotConstantsTest {

  @Nested
  @DisplayName("GDD 2.2 - Allowed Bet Steps")
  class BetStepTests {

    @Test
    @DisplayName("Allowed bet steps match GDD: $0.25 to $250")
    void allowedBetStepsMatchGdd() {
      double[] expected = {
        0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 5.0, 7.5, 10.0, 15.0, 20.0, 25.0, 50.0, 75.0, 100.0,
        150.0, 200.0, 250.0
      };

      assertEquals(expected.length, SlotConstants.ALLOWED_BET_STEPS.length);
      for (int i = 0; i < expected.length; i++) {
        assertEquals(expected[i], SlotConstants.ALLOWED_BET_STEPS[i], 0.001);
      }
    }

    @Test
    @DisplayName("Minimum bet is $0.25 (GDD)")
    void minimumBet() {
      assertEquals(0.25, SlotConstants.ALLOWED_BET_STEPS[0], 0.001);
    }

    @Test
    @DisplayName("Maximum bet is $250 (GDD)")
    void maximumBet() {
      assertEquals(
          250.0,
          SlotConstants.ALLOWED_BET_STEPS[SlotConstants.ALLOWED_BET_STEPS.length - 1],
          0.001);
    }
  }

  @Nested
  @DisplayName("GDD - Symbol ID Constants")
  class SymbolIdTests {

    @Test
    @DisplayName("Low symbols A-D are IDs 1-4")
    void lowSymbolIds() {
      assertEquals(1, SlotConstants.SYMBOL_A);
      assertEquals(2, SlotConstants.SYMBOL_B);
      assertEquals(3, SlotConstants.SYMBOL_C);
      assertEquals(4, SlotConstants.SYMBOL_D);
    }

    @Test
    @DisplayName("Special symbol IDs")
    void specialSymbolIds() {
      assertEquals(9, SlotConstants.DEFAULT_SYMBOL_SCATTER);
      assertEquals(10, SlotConstants.DEFAULT_SYMBOL_WILD);
      assertEquals(11, SlotConstants.SYMBOL_MAJOR);
      assertEquals(12, SlotConstants.SYMBOL_MINI);
      assertEquals(13, SlotConstants.SYMBOL_CASH);
      assertEquals(14, SlotConstants.SYMBOL_STACKED_WILD);
    }
  }

  @Nested
  @DisplayName("GDD - Buy Feature Costs")
  class BuyFeatureCostTests {

    @Test
    @DisplayName("Buy Free Spins cost is 70x (GDD 7.3)")
    void buyFreeSpinsCost() {
      assertEquals(70.0, SlotConstants.BUY_FREE_SPINS_COST, 0.001);
    }

    @Test
    @DisplayName("Buy Hold and Win cost is 70x (GDD 6.3)")
    void buyHoldAndWinCost() {
      assertEquals(70.0, SlotConstants.BUY_HOLD_AND_WIN_COST, 0.001);
    }
  }

  @Nested
  @DisplayName("GDD 9 - Trial Mode")
  class TrialModeTests {

    @Test
    @DisplayName("Trial mode balance is $9,999,999 (GDD 9.3)")
    void trialModeBalance() {
      assertEquals(9999999.0, SlotConstants.TRIAL_MODE_BALANCE, 0.001);
    }
  }

  @Nested
  @DisplayName("GDD 8 - Jackpot Tier Names")
  class JackpotTierTests {

    @Test
    @DisplayName("Jackpot tiers match GDD 8.1")
    void jackpotTiers() {
      assertEquals("DIAMOND", SlotConstants.JACKPOT_DIAMOND);
      assertEquals("RUBY", SlotConstants.JACKPOT_RUBY);
      assertEquals("EMERALD", SlotConstants.JACKPOT_EMERALD);
      assertEquals("SAPPHIRE", SlotConstants.JACKPOT_SAPPHIRE);
    }
  }

  @Nested
  @DisplayName("GDD - Game Modes")
  class GameModeTests {

    @Test
    @DisplayName("Mode constants")
    void modeConstants() {
      assertEquals("base", SlotConstants.MODE_BASE);
      assertEquals("free", SlotConstants.MODE_FREE);
      assertEquals("holdAndWin", SlotConstants.MODE_HOLD_AND_WIN);
    }

    @Test
    @DisplayName("Feature constants")
    void featureConstants() {
      assertEquals("freeSpins", SlotConstants.FEATURE_FREE_SPINS);
      assertEquals("holdAndWin", SlotConstants.FEATURE_HOLD_AND_WIN);
      assertEquals("progressiveJackpot", SlotConstants.FEATURE_JACKPOT);
    }
  }

  @Nested
  @DisplayName("GDD 5.3 - Bonus Type Constants")
  class BonusTypeTests {

    @Test
    @DisplayName("Bonus types: CASH, MINI, MAJOR")
    void bonusTypes() {
      assertEquals("CASH", SlotConstants.TYPE_CASH);
      assertEquals("MINI", SlotConstants.TYPE_MINI);
      assertEquals("MAJOR", SlotConstants.TYPE_MAJOR);
    }
  }
}
