package asia.rgp.game.nagas.modules.slot.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import asia.rgp.game.nagas.shared.domain.model.Money;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SlotStateTest {

  // ==================== FREE SPIN STATE TESTS ====================

  @Nested
  @DisplayName("GDD 4 - Free Spin State Management")
  class FreeSpinStateTests {

    @Test
    @DisplayName("Initial free spin state with 8 spins (GDD 4.3)")
    void initialFreeSpinState() {
      SlotState state =
          SlotState.builder()
              .userId("u1")
              .gameId("g1")
              .sessionId("s1")
              .totalFreeSpins(8)
              .remainingFreeSpins(8)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .build();

      assertTrue(state.isFreeSpinMode());
      assertEquals(8, state.getRemainingFreeSpins());
      assertEquals(8, state.getTotalFreeSpins());
    }

    @Test
    @DisplayName("Consuming a free spin decrements remaining count")
    void consumeFreeSpin() {
      SlotState state =
          SlotState.builder()
              .userId("u1")
              .gameId("g1")
              .sessionId("s1")
              .totalFreeSpins(8)
              .remainingFreeSpins(8)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .build();

      state.consumeFreeSpin();

      assertEquals(7, state.getRemainingFreeSpins());
      assertTrue(state.isFreeSpinMode());
    }

    @Test
    @DisplayName("Free spin mode ends when remaining reaches 0")
    void freeSpinEndsAtZero() {
      SlotState state =
          SlotState.builder()
              .userId("u1")
              .gameId("g1")
              .sessionId("s1")
              .totalFreeSpins(8)
              .remainingFreeSpins(1)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .build();

      state.consumeFreeSpin();

      assertEquals(0, state.getRemainingFreeSpins());
      assertFalse(state.isFreeSpinMode());
    }

    @Test
    @DisplayName("Consuming when already at 0 does not go negative")
    void consumeAtZeroStaysZero() {
      SlotState state =
          SlotState.builder()
              .userId("u1")
              .gameId("g1")
              .sessionId("s1")
              .remainingFreeSpins(0)
              .build();

      state.consumeFreeSpin();

      assertEquals(0, state.getRemainingFreeSpins());
    }

    @Test
    @DisplayName("Re-trigger adds 8 more spins to remaining (GDD 4.4)")
    void retriggerAddsSpins() {
      SlotState state =
          SlotState.builder()
              .userId("u1")
              .gameId("g1")
              .sessionId("s1")
              .totalFreeSpins(8)
              .remainingFreeSpins(3)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .build();

      state.retrigger(8);

      assertEquals(11, state.getRemainingFreeSpins()); // 3 + 8
      assertEquals(16, state.getTotalFreeSpins()); // 8 + 8
      assertTrue(state.isFreeSpinMode());
    }
  }

  // ==================== HOLD AND WIN STATE TESTS ====================

  @Nested
  @DisplayName("GDD 5 - Hold and Win State Management")
  class HoldAndWinStateTests {

    @Test
    @DisplayName("H&W starts with 3 respins (GDD 5.3)")
    void hwStartsWithThreeRespins() {
      SlotState state =
          SlotState.builder()
              .userId("u1")
              .gameId("g1")
              .sessionId("s1")
              .holdAndWin(true)
              .remainingRespins(3)
              .baseBet(Money.of(1.0))
              .build();

      assertTrue(state.isHoldAndWinMode());
      assertEquals(3, state.getRemainingRespins());
    }

    @Test
    @DisplayName("Locked bonus tracks position correctly")
    void lockedBonusPosition() {
      SlotState state =
          SlotState.builder()
              .userId("u1")
              .gameId("g1")
              .sessionId("s1")
              .holdAndWin(true)
              .remainingRespins(3)
              .lockedBonuses(new ArrayList<>())
              .build();

      state.addLockedBonus(new SlotState.LockedBonus(0, 2, 13, 5.0, "CASH"));

      assertTrue(state.isCellLocked(0, 2));
      assertFalse(state.isCellLocked(0, 0));
      assertEquals(1, state.getLockedBonuses().size());
    }

    @Test
    @DisplayName("Full grid = 15 locked bonuses => Grand Bonus (GDD 5.3)")
    void fullGridDetection() {
      SlotState state =
          SlotState.builder()
              .userId("u1")
              .gameId("g1")
              .sessionId("s1")
              .holdAndWin(true)
              .lockedBonuses(new ArrayList<>())
              .build();

      // Fill all 15 cells (3 rows x 5 cols)
      for (int r = 0; r < 3; r++) {
        for (int c = 0; c < 5; c++) {
          state.addLockedBonus(new SlotState.LockedBonus(r, c, 13, 1.0, "CASH"));
        }
      }

      assertTrue(state.isFullGrid());
      assertEquals(15, state.getLockedBonuses().size());
    }

    @Test
    @DisplayName("Not full grid when fewer than 15 bonuses")
    void notFullGrid() {
      SlotState state =
          SlotState.builder()
              .userId("u1")
              .gameId("g1")
              .sessionId("s1")
              .holdAndWin(true)
              .lockedBonuses(new ArrayList<>())
              .build();

      for (int i = 0; i < 14; i++) {
        state.addLockedBonus(new SlotState.LockedBonus(i / 5, i % 5, 13, 1.0, "CASH"));
      }

      assertFalse(state.isFullGrid());
    }

    @Test
    @DisplayName("isCellLocked returns false when no locked bonuses")
    void isCellLockedNullList() {
      SlotState state = SlotState.builder().userId("u1").gameId("g1").sessionId("s1").build();

      assertFalse(state.isCellLocked(0, 0));
    }
  }

  // ==================== WIN ACCUMULATION TESTS ====================

  @Nested
  @DisplayName("Win Accumulation")
  class WinAccumulationTests {

    @Test
    @DisplayName("Accumulated win tracks running total across spins")
    void accumulatedWin() {
      SlotState state =
          SlotState.builder().userId("u1").gameId("g1").sessionId("s1").accumulatedWin(0.0).build();

      state.addWin(5.50);
      assertEquals(5.50, state.getAccumulatedWin(), 0.001);

      state.addWin(3.25);
      assertEquals(8.75, state.getAccumulatedWin(), 0.001);
    }

    @Test
    @DisplayName("Accumulated win is rounded to 2 decimal places")
    void accumulatedWinRounding() {
      SlotState state =
          SlotState.builder().userId("u1").gameId("g1").sessionId("s1").accumulatedWin(0.0).build();

      state.addWin(1.0 / 3.0); // 0.33333...
      assertEquals(0.33, state.getAccumulatedWin(), 0.001);
    }
  }

  // ==================== MODE DETECTION TESTS ====================

  @Nested
  @DisplayName("Mode Detection")
  class ModeDetectionTests {

    @Test
    @DisplayName("Default state is base mode (no FS, no H&W)")
    void defaultBaseMode() {
      SlotState state = SlotState.builder().userId("u1").gameId("g1").sessionId("s1").build();

      assertFalse(state.isFreeSpinMode());
      assertFalse(state.isHoldAndWinMode());
    }

    @Test
    @DisplayName("isHoldAndWinMode depends on holdAndWin boolean flag")
    void holdAndWinModeFlag() {
      SlotState state =
          SlotState.builder().userId("u1").gameId("g1").sessionId("s1").holdAndWin(true).build();

      assertTrue(state.isHoldAndWinMode());

      state.setHoldAndWin(false);
      assertFalse(state.isHoldAndWinMode());
    }

    @Test
    @DisplayName("isFreeSpinMode depends on remainingFreeSpins > 0")
    void freeSpinModeFromRemaining() {
      SlotState state =
          SlotState.builder()
              .userId("u1")
              .gameId("g1")
              .sessionId("s1")
              .remainingFreeSpins(1)
              .freeSpinMode(true)
              .build();

      assertTrue(state.isFreeSpinMode());

      state.consumeFreeSpin();
      assertFalse(state.isFreeSpinMode());
    }
  }
}
