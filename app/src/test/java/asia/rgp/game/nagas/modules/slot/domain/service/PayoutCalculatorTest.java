package asia.rgp.game.nagas.modules.slot.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import asia.rgp.game.nagas.modules.slot.domain.model.*;
import asia.rgp.game.nagas.shared.domain.model.Matrix;
import asia.rgp.game.nagas.shared.domain.model.Money;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PayoutCalculatorTest {

  private PayoutCalculator calculator;
  private SlotGameConfig config;

  // Symbol IDs matching GDD
  static final int SYM_A = 1, SYM_B = 2, SYM_C = 3, SYM_D = 4;
  static final int SYM_E = 5, SYM_F = 6, SYM_G = 7, SYM_H = 8;
  static final int SYM_SCATTER = 9, SYM_WILD = 10;
  static final int SYM_MAJOR = 11, SYM_MINI = 12, SYM_CASH = 13;
  static final int SYM_STACKED_WILD = 14;

  @BeforeEach
  void setUp() {
    calculator = new PayoutCalculator();
    config = buildTestConfig();
  }

  // ==================== LINE WIN TESTS ====================

  @Nested
  @DisplayName("GDD 2.3 - Line Win Calculation")
  class LineWinTests {

    @Test
    @DisplayName("3 matching symbols A from left pays 0.2 x totalBet")
    void threeMatchSymbolA() {
      // Row 1 (middle): A, A, A, D, D
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_A, SYM_A, SYM_A, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);
      Money totalBet = Money.of(1.0);

      PayoutResult result = calculator.calculate(matrix, config, totalBet);

      // Payline 1 = [1,1,1,1,1] => A,A,A,D,D => 3-match A => 0.2 x 1.0 = 0.2
      assertTrue(result.getTotalWin().getAmount() > 0);
      WinDetail lineWin =
          result.getWins().stream()
              .filter(w -> "line".equals(w.getType()))
              .findFirst()
              .orElse(null);
      assertNotNull(lineWin);
      assertEquals(SYM_A, lineWin.getSymbolId());
      assertEquals(3, lineWin.getCount());
      assertEquals(0.2, lineWin.getAmount().getAmount(), 0.001);
    }

    @Test
    @DisplayName("4 matching symbols B from left pays 0.4 x totalBet")
    void fourMatchSymbolB() {
      int[][] grid = {
        {SYM_B, SYM_B, SYM_B, SYM_B, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);
      Money totalBet = Money.of(2.0);

      PayoutResult result = calculator.calculate(matrix, config, totalBet);

      // Payline 2 = [0,0,0,0,0] => B,B,B,B,D => 4-match B => 0.4 x 2.0 = 0.8
      WinDetail win =
          result.getWins().stream()
              .filter(w -> w.getSymbolId() == SYM_B && w.getCount() == 4)
              .findFirst()
              .orElse(null);
      assertNotNull(win);
      assertEquals(0.8, win.getAmount().getAmount(), 0.001);
    }

    @Test
    @DisplayName("5 matching symbols H from left pays 10.0 x totalBet")
    void fiveMatchSymbolH() {
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_H, SYM_H, SYM_H, SYM_H, SYM_H},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);
      Money totalBet = Money.of(1.0);

      PayoutResult result = calculator.calculate(matrix, config, totalBet);

      // Payline 1 = [1,1,1,1,1] => 5-match H => 10.0 x 1.0 = 10.0
      WinDetail win =
          result.getWins().stream()
              .filter(w -> w.getSymbolId() == SYM_H && w.getCount() == 5)
              .findFirst()
              .orElse(null);
      assertNotNull(win);
      assertEquals(10.0, win.getAmount().getAmount(), 0.001);
    }

    @Test
    @DisplayName("High symbols paytable: E(0.4/1/4), F(0.4/2/6), G(0.4/3/8)")
    void highSymbolPaytable() {
      // 5-match E on payline 1
      int[][] gridE = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_E, SYM_E, SYM_E, SYM_E, SYM_E},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      PayoutResult resultE = calculator.calculate(new Matrix(3, 5, gridE), config, Money.of(1.0));
      assertEquals(
          4.0,
          resultE.getWins().stream()
              .filter(w -> w.getSymbolId() == SYM_E)
              .findFirst()
              .get()
              .getAmount()
              .getAmount(),
          0.001);

      // 5-match F
      int[][] gridF = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_F, SYM_F, SYM_F, SYM_F, SYM_F},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      PayoutResult resultF = calculator.calculate(new Matrix(3, 5, gridF), config, Money.of(1.0));
      assertEquals(
          6.0,
          resultF.getWins().stream()
              .filter(w -> w.getSymbolId() == SYM_F)
              .findFirst()
              .get()
              .getAmount()
              .getAmount(),
          0.001);

      // 5-match G
      int[][] gridG = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_G, SYM_G, SYM_G, SYM_G, SYM_G},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      PayoutResult resultG = calculator.calculate(new Matrix(3, 5, gridG), config, Money.of(1.0));
      assertEquals(
          8.0,
          resultG.getWins().stream()
              .filter(w -> w.getSymbolId() == SYM_G)
              .findFirst()
              .get()
              .getAmount()
              .getAmount(),
          0.001);
    }

    @Test
    @DisplayName("Payout uses totalBet, not betPerLine (GDD: Payout = Multiplier x Bet)")
    void payoutUsesTotalBet() {
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_A, SYM_A, SYM_A, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);
      Money totalBet = Money.of(25.0);

      PayoutResult result = calculator.calculate(matrix, config, totalBet);

      // 3-match A: 0.2 x 25.0 = 5.0 (NOT 0.2 x 25/25 = 0.2)
      WinDetail win =
          result.getWins().stream()
              .filter(w -> w.getSymbolId() == SYM_A && w.getCount() == 3)
              .findFirst()
              .orElse(null);
      assertNotNull(win);
      assertEquals(5.0, win.getAmount().getAmount(), 0.001);
    }

    @Test
    @DisplayName("No win when fewer than 3 matching symbols")
    void noWinLessThanThreeMatch() {
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_A, SYM_A, SYM_D, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      List<WinDetail> lineWins =
          result.getWins().stream().filter(w -> "line".equals(w.getType())).toList();
      // Payline 1 => A, A, D => only 2 match => no win on that line
      assertTrue(lineWins.stream().noneMatch(w -> w.getSymbolId() == SYM_A && w.getLineId() == 1));
    }

    @Test
    @DisplayName("Only highest win per payline is paid (GDD 2.3)")
    void highestWinPerPayline() {
      // Payline 1 = [1,1,1,1,1]: H,H,H,H,H = 5-match H
      // Verifies 5-match is taken, not 3-match or 4-match separately
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_H, SYM_H, SYM_H, SYM_H, SYM_H},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      long hWins =
          result.getWins().stream()
              .filter(w -> w.getSymbolId() == SYM_H && w.getLineId() == 1)
              .count();
      // Should be exactly 1 win for this payline (the 5-match)
      assertEquals(1, hWins);
      assertEquals(
          5,
          result.getWins().stream()
              .filter(w -> w.getSymbolId() == SYM_H && w.getLineId() == 1)
              .findFirst()
              .get()
              .getCount());
    }

    @Test
    @DisplayName("Line sequence breaks at Scatter symbol")
    void lineBreaksAtScatter() {
      // Payline 1 = [1,1,1,1,1]: A, A, SCATTER, A, A
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_A, SYM_A, SYM_SCATTER, SYM_A, SYM_A},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      // Line breaks at scatter: only 2-match A => no line win for payline 1
      assertTrue(
          result.getWins().stream()
              .noneMatch(w -> "line".equals(w.getType()) && w.getLineId() == 1));
    }

    @Test
    @DisplayName("Line sequence breaks at Bonus symbol")
    void lineBreaksAtBonus() {
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_H, SYM_H, SYM_CASH, SYM_H, SYM_H},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      assertTrue(
          result.getWins().stream()
              .noneMatch(w -> "line".equals(w.getType()) && w.getLineId() == 1));
    }
  }

  // ==================== WILD SUBSTITUTION TESTS ====================

  @Nested
  @DisplayName("GDD 3.1 - Wild Substitution")
  class WildTests {

    @Test
    @DisplayName("Wild (10) substitutes for regular symbols on payline")
    void wildSubstitutesForRegular() {
      // Payline 1: A, WILD, A, A, A => 5-match A with wild sub
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_A, SYM_WILD, SYM_A, SYM_A, SYM_A},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      WinDetail win =
          result.getWins().stream()
              .filter(w -> w.getLineId() == 1 && w.getSymbolId() == SYM_A)
              .findFirst()
              .orElse(null);
      assertNotNull(win, "Wild should substitute for A");
      assertEquals(5, win.getCount());
      assertEquals(2.0, win.getAmount().getAmount(), 0.001); // 5-match A = 2.0 x 1.0
    }

    @Test
    @DisplayName("Wild does NOT substitute for Scatter (GDD 3.1)")
    void wildDoesNotSubstituteScatter() {
      // Wild cannot replace Scatter, so line breaks
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_SCATTER, SYM_WILD, SYM_D, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      // Scatter at position 0 breaks line immediately
      assertTrue(
          result.getWins().stream()
              .noneMatch(w -> "line".equals(w.getType()) && w.getLineId() == 1));
    }

    @Test
    @DisplayName("Leading wilds adopt the first non-wild symbol")
    void leadingWildsAdoptSymbol() {
      // Payline 1: WILD, WILD, H, H, H => Should be 5-match H
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_WILD, SYM_WILD, SYM_H, SYM_H, SYM_H},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      WinDetail win =
          result.getWins().stream()
              .filter(w -> w.getLineId() == 1 && w.getSymbolId() == SYM_H)
              .findFirst()
              .orElse(null);
      assertNotNull(win, "Leading wilds should adopt H");
      assertEquals(5, win.getCount());
      assertEquals(10.0, win.getAmount().getAmount(), 0.001);
    }
  }

  // ==================== SCATTER WIN TESTS ====================

  @Nested
  @DisplayName("GDD 3.2 / 4.2 - Scatter Wins & Free Spin Trigger")
  class ScatterTests {

    @Test
    @DisplayName("3 Scatters pay 2.0 x totalBet (GDD 3.2)")
    void threeScattersPay() {
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_D, SYM_SCATTER, SYM_SCATTER, SYM_SCATTER, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      WinDetail scatterWin =
          result.getWins().stream()
              .filter(w -> "scatter".equals(w.getType()))
              .findFirst()
              .orElse(null);
      assertNotNull(scatterWin);
      assertEquals(3, scatterWin.getCount());
      assertEquals(2.0, scatterWin.getAmount().getAmount(), 0.001); // 2.0 x 1.0
    }

    @Test
    @DisplayName("3 Scatters trigger 8 Free Spins (GDD 4.2/4.3)")
    void threeScattersTriggerFreeSpins() {
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_D, SYM_SCATTER, SYM_SCATTER, SYM_SCATTER, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      assertTrue(result.isTriggerFreeSpin());
      assertEquals(8, result.getFreeSpinCount());
    }

    @Test
    @DisplayName("2 Scatters do NOT trigger Free Spins")
    void twoScattersNoTrigger() {
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_D, SYM_SCATTER, SYM_SCATTER, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      assertFalse(result.isTriggerFreeSpin());
      assertEquals(0, result.getFreeSpinCount());
    }

    @Test
    @DisplayName("Scatter win uses totalBet (GDD: Scatter Reward = Scatter Multiplier x Bet)")
    void scatterWinUsesTotalBet() {
      int[][] grid = {
        {SYM_D, SYM_SCATTER, SYM_D, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_SCATTER, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_SCATTER, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);
      Money totalBet = Money.of(10.0);

      PayoutResult result = calculator.calculate(matrix, config, totalBet);

      WinDetail scatter =
          result.getWins().stream()
              .filter(w -> "scatter".equals(w.getType()))
              .findFirst()
              .orElse(null);
      assertNotNull(scatter);
      // 3 scatter x 2.0 multiplier x $10 totalBet = $20
      assertEquals(20.0, scatter.getAmount().getAmount(), 0.001);
    }
  }

  // ==================== HOLD AND WIN TRIGGER TESTS ====================

  @Nested
  @DisplayName("GDD 5.2 - Hold and Win Trigger")
  class HoldAndWinTriggerTests {

    @Test
    @DisplayName("6 Bonus symbols trigger Hold and Win")
    void sixBonusTriggerHW() {
      int[][] grid = {
        {SYM_CASH, SYM_CASH, SYM_CASH, SYM_D, SYM_D},
        {SYM_CASH, SYM_CASH, SYM_CASH, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      assertTrue(result.isTriggerHoldAndWin());
      assertEquals(6, result.getBonusCount());
    }

    @Test
    @DisplayName("5 Bonus symbols do NOT trigger Hold and Win")
    void fiveBonusNoTrigger() {
      int[][] grid = {
        {SYM_CASH, SYM_CASH, SYM_CASH, SYM_D, SYM_D},
        {SYM_CASH, SYM_CASH, SYM_D, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      assertFalse(result.isTriggerHoldAndWin());
      assertEquals(5, result.getBonusCount());
    }

    @Test
    @DisplayName("Major bonus has fixed multiplier 100x (GDD 5.3)")
    void majorBonusMultiplier() {
      int[][] grid = {
        {SYM_MAJOR, SYM_CASH, SYM_CASH, SYM_CASH, SYM_CASH},
        {SYM_CASH, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      PayoutResult.BonusInfo majorBonus =
          result.getBonusInfos().stream()
              .filter(b -> SlotConstants.TYPE_MAJOR.equals(b.getType()))
              .findFirst()
              .orElse(null);
      assertNotNull(majorBonus);
      assertEquals(100.0, majorBonus.getMultiplier(), 0.001);
    }

    @Test
    @DisplayName("Mini bonus has fixed multiplier 25x (GDD 5.3)")
    void miniBonusMultiplier() {
      int[][] grid = {
        {SYM_MINI, SYM_CASH, SYM_CASH, SYM_CASH, SYM_CASH},
        {SYM_CASH, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      PayoutResult.BonusInfo miniBonus =
          result.getBonusInfos().stream()
              .filter(b -> SlotConstants.TYPE_MINI.equals(b.getType()))
              .findFirst()
              .orElse(null);
      assertNotNull(miniBonus);
      assertEquals(25.0, miniBonus.getMultiplier(), 0.001);
    }

    @Test
    @DisplayName("Cash bonus multiplier is from allowed options (GDD 3.2)")
    void cashBonusMultiplierFromOptions() {
      double[] allowedOptions = {1.0, 2.0, 3.0, 4.0, 5.0, 7.0, 10.0, 12.0, 15.0, 18.0, 20.0};
      for (int i = 0; i < 50; i++) {
        double mult = calculator.generateRandomCashMultiplier();
        boolean found = false;
        for (double opt : allowedOptions) {
          if (Math.abs(mult - opt) < 0.001) {
            found = true;
            break;
          }
        }
        assertTrue(found, "Multiplier " + mult + " not in allowed options");
      }
    }
  }

  // ==================== WIN CAP TESTS ====================

  @Nested
  @DisplayName("GDD 2.3 - Win Cap (2000x)")
  class WinCapTests {

    @Test
    @DisplayName("Win is capped at 2000 x Bet")
    void winCappedAt2000xBet() {
      // Create a config with small maxWinMultiplier for testing
      SlotGameConfig smallCapConfig = buildTestConfigWithMaxWin(10.0);

      // 5-match H = 10.0 x 1.0 = $10. But if cap is 10x => cap = $10
      // Multiple paylines winning should exceed cap
      // Row 0: H,H,H,H,H  Row 1: H,H,H,H,H  Row 2: H,H,H,H,H
      int[][] grid = {
        {SYM_H, SYM_H, SYM_H, SYM_H, SYM_H},
        {SYM_H, SYM_H, SYM_H, SYM_H, SYM_H},
        {SYM_H, SYM_H, SYM_H, SYM_H, SYM_H}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      // maxWinMultiplier = 2000.0 in default config. All 25 paylines hit 5H = 25 * 10 = $250.
      // $250 < $2000, so no cap here.
      assertTrue(result.getTotalWin().getAmount() <= 2000.0);

      // Now test with small cap
      PayoutResult cappedResult = calculator.calculate(matrix, smallCapConfig, Money.of(1.0));
      // 25 paylines * 10.0 = $250, cap at 10x$1 = $10
      assertEquals(10.0, cappedResult.getTotalWin().getAmount(), 0.001);
    }

    @Test
    @DisplayName("GDD formula: Final Payout = min(Payout, 2000 x Bet)")
    void finalPayoutFormula() {
      SlotGameConfig capConfig = buildTestConfigWithMaxWin(5.0);

      // 5-match H on middle row = 10.0 x $2 = $20. Cap = 5 x $2 = $10.
      int[][] grid = {
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D},
        {SYM_H, SYM_H, SYM_H, SYM_H, SYM_H},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, capConfig, Money.of(2.0));

      assertEquals(10.0, result.getTotalWin().getAmount(), 0.001);
    }
  }

  // ==================== JACKPOT TRIGGER TESTS ====================

  @Nested
  @DisplayName("GDD 8.2 - Jackpot Trigger (Glowing Rings)")
  class JackpotTriggerTests {

    @Test
    @DisplayName("6+ Glowing Rings trigger Jackpot")
    void sixGlowingRingsTrigger() {
      int[][] grid = {
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A},
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A},
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A}
      };
      Matrix matrix = new Matrix(3, 5, grid);
      // Manually set 6 glowing rings
      for (int c = 0; c < 5; c++) {
        matrix.setOverlayAt(0, c, "GLOWING_RING", true);
      }
      matrix.setOverlayAt(1, 0, "GLOWING_RING", true);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      assertTrue(result.isJackpotTriggered());
      assertEquals(6, result.getGlowingRingCount());
    }

    @Test
    @DisplayName("5 Glowing Rings do NOT trigger Jackpot")
    void fiveGlowingRingsNoTrigger() {
      int[][] grid = {
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A},
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A},
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A}
      };
      Matrix matrix = new Matrix(3, 5, grid);
      for (int c = 0; c < 5; c++) {
        matrix.setOverlayAt(0, c, "GLOWING_RING", true);
      }

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      assertFalse(result.isJackpotTriggered());
    }

    @Test
    @DisplayName("Glowing Rings NOT applied to Scatter, Bonus, StackedWild (GDD 8.2)")
    void glowingRingsNotOnSpecialSymbols() {
      int[][] grid = {
        {SYM_SCATTER, SYM_CASH, SYM_STACKED_WILD, SYM_MAJOR, SYM_MINI},
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A},
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A}
      };
      Matrix matrix = new Matrix(3, 5, grid);
      // Set glowing ring on all row 0 cells (special symbols)
      for (int c = 0; c < 5; c++) {
        matrix.setOverlayAt(0, c, "GLOWING_RING", true);
      }

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      // Only normal symbols (row 1 & 2 cells) can have valid glowing rings
      // Row 0 cells with scatter/bonus/stacked_wild should be excluded
      assertEquals(0, result.getGlowingRingCount());
    }
  }

  // ==================== COMBINED SCENARIO TESTS ====================

  @Nested
  @DisplayName("GDD - Combined Scenarios")
  class CombinedTests {

    @Test
    @DisplayName("No wins on empty grid")
    void noWinsOnBlankGrid() {
      int[][] grid = {
        {0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      assertEquals(0.0, result.getTotalWin().getAmount(), 0.001);
      assertFalse(result.isTriggerFreeSpin());
      assertFalse(result.isTriggerHoldAndWin());
      assertFalse(result.isJackpotTriggered());
    }

    @Test
    @DisplayName("Line wins + Scatter wins aggregate correctly")
    void lineAndScatterAggregate() {
      // Payline 1 [1,1,1,1,1]: H,H,H,H,H = 10.0
      // 3 Scatters on row 0 cols 1,2,3 = 2.0
      int[][] grid = {
        {SYM_D, SYM_SCATTER, SYM_SCATTER, SYM_SCATTER, SYM_D},
        {SYM_H, SYM_H, SYM_H, SYM_H, SYM_H},
        {SYM_D, SYM_D, SYM_D, SYM_D, SYM_D}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      PayoutResult result = calculator.calculate(matrix, config, Money.of(1.0));

      // Should have both line and scatter wins
      assertTrue(result.getWins().stream().anyMatch(w -> "line".equals(w.getType())));
      assertTrue(result.getWins().stream().anyMatch(w -> "scatter".equals(w.getType())));
      assertTrue(result.isTriggerFreeSpin());
      // Total = line wins + scatter win (2.0)
      assertTrue(result.getTotalWin().getAmount() > 2.0);
    }

    @Test
    @DisplayName("PayoutResult.empty() returns zero-state")
    void emptyPayoutResult() {
      PayoutResult empty = PayoutResult.empty();

      assertEquals(0.0, empty.getTotalWin().getAmount());
      assertTrue(empty.getWins().isEmpty());
      assertFalse(empty.isTriggerFreeSpin());
      assertFalse(empty.isTriggerHoldAndWin());
      assertFalse(empty.isJackpotTriggered());
      assertTrue(empty.getBonusInfos().isEmpty());
      assertTrue(empty.getGlowingRingPositions().isEmpty());
    }
  }

  // ==================== TEST CONFIG BUILDERS ====================

  private SlotGameConfig buildTestConfig() {
    return buildTestConfigWithMaxWin(2000.0);
  }

  private SlotGameConfig buildTestConfigWithMaxWin(double maxWin) {
    Map<Integer, SlotSymbol> symbols =
        Map.ofEntries(
            Map.entry(
                SYM_A, new SlotSymbol(SYM_A, "A", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
            Map.entry(
                SYM_B, new SlotSymbol(SYM_B, "B", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
            Map.entry(
                SYM_C, new SlotSymbol(SYM_C, "C", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
            Map.entry(
                SYM_D, new SlotSymbol(SYM_D, "D", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
            Map.entry(
                SYM_E, new SlotSymbol(SYM_E, "E", Map.of(3, 0.4, 4, 1.0, 5, 4.0), false, false)),
            Map.entry(
                SYM_F, new SlotSymbol(SYM_F, "F", Map.of(3, 0.4, 4, 2.0, 5, 6.0), false, false)),
            Map.entry(
                SYM_G, new SlotSymbol(SYM_G, "G", Map.of(3, 0.4, 4, 3.0, 5, 8.0), false, false)),
            Map.entry(
                SYM_H, new SlotSymbol(SYM_H, "H", Map.of(3, 1.0, 4, 4.0, 5, 10.0), false, false)),
            Map.entry(
                SYM_SCATTER, new SlotSymbol(SYM_SCATTER, "SCATTER", Map.of(3, 2.0), false, true)),
            Map.entry(SYM_WILD, new SlotSymbol(SYM_WILD, "WILD", Map.of(), true, false)),
            Map.entry(SYM_MAJOR, new SlotSymbol(SYM_MAJOR, "MAJOR", Map.of(), false, false)),
            Map.entry(SYM_MINI, new SlotSymbol(SYM_MINI, "MINI", Map.of(), false, false)),
            Map.entry(SYM_CASH, new SlotSymbol(SYM_CASH, "BONUS", Map.of(), false, false)),
            Map.entry(
                SYM_STACKED_WILD,
                new SlotSymbol(SYM_STACKED_WILD, "STACKED_WILD", Map.of(), true, false)));

    List<Payline> paylines =
        List.of(
            new Payline(1, new int[] {1, 1, 1, 1, 1}),
            new Payline(2, new int[] {0, 0, 0, 0, 0}),
            new Payline(3, new int[] {2, 2, 2, 2, 2}),
            new Payline(4, new int[] {0, 1, 2, 1, 0}),
            new Payline(5, new int[] {2, 1, 0, 1, 2}),
            new Payline(6, new int[] {1, 0, 0, 0, 1}),
            new Payline(7, new int[] {1, 2, 2, 2, 1}),
            new Payline(8, new int[] {0, 0, 1, 2, 2}),
            new Payline(9, new int[] {2, 2, 1, 0, 0}),
            new Payline(10, new int[] {1, 2, 1, 0, 1}),
            new Payline(11, new int[] {1, 0, 1, 2, 1}),
            new Payline(12, new int[] {0, 1, 1, 1, 0}),
            new Payline(13, new int[] {2, 1, 1, 1, 2}),
            new Payline(14, new int[] {0, 1, 0, 1, 0}),
            new Payline(15, new int[] {2, 1, 2, 1, 2}),
            new Payline(16, new int[] {1, 1, 0, 1, 1}),
            new Payline(17, new int[] {1, 1, 2, 1, 1}),
            new Payline(18, new int[] {0, 0, 2, 0, 0}),
            new Payline(19, new int[] {2, 2, 0, 2, 2}),
            new Payline(20, new int[] {0, 2, 2, 2, 0}),
            new Payline(21, new int[] {2, 0, 0, 0, 2}),
            new Payline(22, new int[] {1, 2, 0, 2, 1}),
            new Payline(23, new int[] {1, 0, 2, 0, 1}),
            new Payline(24, new int[] {0, 2, 0, 2, 0}),
            new Payline(25, new int[] {2, 0, 2, 0, 2}));

    // Minimal reel strips for config validation
    List<List<Integer>> reelStrips =
        List.of(
            List.of(1, 2, 3, 4, 5, 6, 7, 8, 10),
            List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 14),
            List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 13, 14),
            List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 13, 14),
            List.of(1, 2, 3, 4, 5, 6, 7, 8, 10, 13, 14));

    return new SlotGameConfig(
        "test_game",
        3,
        5,
        PayoutType.LINE,
        reelStrips,
        paylines,
        symbols,
        SYM_SCATTER,
        SYM_WILD,
        SYM_CASH,
        SYM_MAJOR,
        SYM_MINI,
        3,
        8,
        maxWin);
  }
}
