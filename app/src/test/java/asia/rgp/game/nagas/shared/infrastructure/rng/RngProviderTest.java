package asia.rgp.game.nagas.shared.infrastructure.rng;

import static org.junit.jupiter.api.Assertions.*;

import asia.rgp.game.nagas.modules.slot.domain.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RngProviderTest {

  private RngProvider rngProvider;
  private SlotGameConfig config;

  static final int SYM_A = 1, SYM_B = 2, SYM_C = 3, SYM_D = 4;
  static final int SYM_E = 5, SYM_F = 6, SYM_G = 7, SYM_H = 8;
  static final int SYM_SCATTER = 9, SYM_WILD = 10;
  static final int SYM_MAJOR = 11, SYM_MINI = 12, SYM_CASH = 13;
  static final int SYM_STACKED_WILD = 14;

  @BeforeEach
  void setUp() {
    rngProvider = new RngProvider();
    config = buildTestConfig();
  }

  // ==================== BASE GRID GENERATION ====================

  @Nested
  @DisplayName("GDD 2.1 - Base Grid Generation")
  class BaseGridTests {

    @Test
    @DisplayName("Generated grid has correct dimensions (3 rows x 5 cols)")
    void gridDimensions() {
      int[][] grid = rngProvider.generateGridFromStrips(config, false);

      assertEquals(3, grid.length);
      for (int[] row : grid) {
        assertEquals(5, row.length);
      }
    }

    @Test
    @DisplayName("Grid symbols come from reel strips")
    void gridSymbolsFromStrips() {
      Set<Integer> allStripSymbols =
          Set.of(
              SYM_A,
              SYM_B,
              SYM_C,
              SYM_D,
              SYM_E,
              SYM_F,
              SYM_G,
              SYM_H,
              SYM_SCATTER,
              SYM_WILD,
              SYM_MAJOR,
              SYM_MINI,
              SYM_CASH,
              SYM_STACKED_WILD);

      for (int i = 0; i < 20; i++) {
        int[][] grid = rngProvider.generateGridFromStrips(config, false);
        for (int r = 0; r < 3; r++) {
          for (int c = 0; c < 5; c++) {
            assertTrue(
                allStripSymbols.contains(grid[r][c]),
                "Symbol " + grid[r][c] + " at [" + r + "," + c + "] not in reel strips");
          }
        }
      }
    }
  }

  // ==================== FREE SPIN FILTERING ====================

  @Nested
  @DisplayName("GDD 4.4 - Free Spin Strip Filtering")
  class FreeSpinFilterTests {

    @Test
    @DisplayName("Free Spin grid excludes symbols A, B, C, D (GDD 4.4)")
    void freeSpinExcludesLowSymbols() {
      Set<Integer> forbidden = Set.of(SYM_A, SYM_B, SYM_C, SYM_D);

      for (int i = 0; i < 50; i++) {
        int[][] grid = rngProvider.generateGridFromStrips(config, true);
        for (int r = 0; r < 3; r++) {
          for (int c = 0; c < 5; c++) {
            assertFalse(
                forbidden.contains(grid[r][c]),
                "Low symbol " + grid[r][c] + " found at [" + r + "," + c + "] during Free Spin");
          }
        }
      }
    }

    @Test
    @DisplayName("Free Spin grid excludes Major(11) and Mini(12) symbols (GDD 4.4)")
    void freeSpinExcludesMajorMini() {
      Set<Integer> forbidden = Set.of(SYM_MAJOR, SYM_MINI);

      for (int i = 0; i < 50; i++) {
        int[][] grid = rngProvider.generateGridFromStrips(config, true);
        for (int r = 0; r < 3; r++) {
          for (int c = 0; c < 5; c++) {
            assertFalse(
                forbidden.contains(grid[r][c]),
                "Major/Mini symbol "
                    + grid[r][c]
                    + " found at ["
                    + r
                    + ","
                    + c
                    + "] during Free Spin");
          }
        }
      }
    }

    @Test
    @DisplayName("Free Spin grid only contains E, F, G, H, Wild, Scatter, Bonus (GDD 4.4)")
    void freeSpinOnlyAllowedSymbols() {
      Set<Integer> allowed =
          Set.of(SYM_E, SYM_F, SYM_G, SYM_H, SYM_WILD, SYM_SCATTER, SYM_CASH, SYM_STACKED_WILD);

      for (int i = 0; i < 50; i++) {
        int[][] grid = rngProvider.generateGridFromStrips(config, true);
        for (int r = 0; r < 3; r++) {
          for (int c = 0; c < 5; c++) {
            assertTrue(
                allowed.contains(grid[r][c]),
                "Symbol " + grid[r][c] + " at [" + r + "," + c + "] not in allowed FS set");
          }
        }
      }
    }
  }

  // ==================== STACKED WILD EXPANSION ====================

  @Nested
  @DisplayName("GDD 3.1 - Stacked Wild Expansion")
  class StackedWildTests {

    @Test
    @DisplayName("Stacked Wild (14) expands entire column to Wild (10)")
    void stackedWildExpandsColumn() {
      int[][] grid = {
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A},
        {SYM_A, SYM_STACKED_WILD, SYM_A, SYM_A, SYM_A},
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A}
      };

      rngProvider.applyStackedWildExpansion(grid, config);

      // Column 1 should be entirely wild (10)
      assertEquals(SYM_WILD, grid[0][1]);
      assertEquals(SYM_WILD, grid[1][1]);
      assertEquals(SYM_WILD, grid[2][1]);

      // Other columns unchanged
      assertEquals(SYM_A, grid[0][0]);
      assertEquals(SYM_A, grid[0][2]);
    }

    @Test
    @DisplayName("No expansion when no stacked wild present")
    void noExpansionWithoutStackedWild() {
      int[][] grid = {
        {SYM_A, SYM_B, SYM_C, SYM_D, SYM_E},
        {SYM_F, SYM_G, SYM_H, SYM_A, SYM_B},
        {SYM_C, SYM_D, SYM_E, SYM_F, SYM_G}
      };
      int[][] original = {
        {SYM_A, SYM_B, SYM_C, SYM_D, SYM_E},
        {SYM_F, SYM_G, SYM_H, SYM_A, SYM_B},
        {SYM_C, SYM_D, SYM_E, SYM_F, SYM_G}
      };

      rngProvider.applyStackedWildExpansion(grid, config);

      assertArrayEquals(original[0], grid[0]);
      assertArrayEquals(original[1], grid[1]);
      assertArrayEquals(original[2], grid[2]);
    }

    @Test
    @DisplayName("Multiple columns can expand independently")
    void multipleColumnsExpand() {
      int[][] grid = {
        {SYM_A, SYM_STACKED_WILD, SYM_A, SYM_STACKED_WILD, SYM_A},
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A},
        {SYM_A, SYM_A, SYM_A, SYM_A, SYM_A}
      };

      rngProvider.applyStackedWildExpansion(grid, config);

      // Columns 1 and 3 fully wild
      for (int r = 0; r < 3; r++) {
        assertEquals(SYM_WILD, grid[r][1]);
        assertEquals(SYM_WILD, grid[r][3]);
      }
      // Columns 0, 2, 4 unchanged
      for (int r = 0; r < 3; r++) {
        assertEquals(SYM_A, grid[r][0]);
        assertEquals(SYM_A, grid[r][2]);
        assertEquals(SYM_A, grid[r][4]);
      }
    }
  }

  // ==================== HOLD AND WIN GRID ====================

  @Nested
  @DisplayName("GDD 5.3 - Hold and Win Grid Generation")
  class HoldAndWinGridTests {

    @Test
    @DisplayName("H&W grid preserves locked bonus positions")
    void hwGridPreservesLockedPositions() {
      List<SlotState.LockedBonus> locked = new ArrayList<>();
      locked.add(new SlotState.LockedBonus(0, 0, SYM_CASH, 5.0, "CASH"));
      locked.add(new SlotState.LockedBonus(1, 2, SYM_MAJOR, 100.0, "MAJOR"));
      locked.add(new SlotState.LockedBonus(2, 4, SYM_MINI, 25.0, "MINI"));

      int[][] grid = rngProvider.generateHoldAndWinGrid(config, locked);

      assertEquals(SYM_CASH, grid[0][0]);
      assertEquals(SYM_MAJOR, grid[1][2]);
      assertEquals(SYM_MINI, grid[2][4]);
    }

    @Test
    @DisplayName("H&W grid non-locked cells are 0 or bonus symbols")
    void hwGridNonLockedCells() {
      List<SlotState.LockedBonus> locked = new ArrayList<>();
      locked.add(new SlotState.LockedBonus(0, 0, SYM_CASH, 5.0, "CASH"));

      Set<Integer> validSymbols = Set.of(0, SYM_CASH, SYM_MAJOR, SYM_MINI);

      for (int i = 0; i < 50; i++) {
        int[][] grid = rngProvider.generateHoldAndWinGrid(config, locked);
        for (int r = 0; r < 3; r++) {
          for (int c = 0; c < 5; c++) {
            if (r == 0 && c == 0) continue; // Skip locked cell
            assertTrue(
                validSymbols.contains(grid[r][c]),
                "Invalid symbol " + grid[r][c] + " at [" + r + "," + c + "] in H&W grid");
          }
        }
      }
    }

    @Test
    @DisplayName("H&W grid does not add Major if already locked (GDD 5.3)")
    void hwNoSecondMajor() {
      List<SlotState.LockedBonus> locked = new ArrayList<>();
      locked.add(new SlotState.LockedBonus(0, 0, SYM_MAJOR, 100.0, "MAJOR"));
      // Fill some to have existing bonuses
      for (int i = 1; i < 6; i++) {
        locked.add(new SlotState.LockedBonus(i / 5, i % 5, SYM_CASH, 5.0, "CASH"));
      }

      for (int i = 0; i < 100; i++) {
        int[][] grid = rngProvider.generateHoldAndWinGrid(config, locked);
        int majorCount = 0;
        for (int r = 0; r < 3; r++) {
          for (int c = 0; c < 5; c++) {
            if (grid[r][c] == SYM_MAJOR) majorCount++;
          }
        }
        assertTrue(majorCount <= 1, "Found " + majorCount + " Major symbols, max is 1");
      }
    }

    @Test
    @DisplayName("H&W grid does not add Mini if already locked (GDD 5.3)")
    void hwNoSecondMini() {
      List<SlotState.LockedBonus> locked = new ArrayList<>();
      locked.add(new SlotState.LockedBonus(0, 0, SYM_MINI, 25.0, "MINI"));
      for (int i = 1; i < 6; i++) {
        locked.add(new SlotState.LockedBonus(i / 5, i % 5, SYM_CASH, 5.0, "CASH"));
      }

      for (int i = 0; i < 100; i++) {
        int[][] grid = rngProvider.generateHoldAndWinGrid(config, locked);
        int miniCount = 0;
        for (int r = 0; r < 3; r++) {
          for (int c = 0; c < 5; c++) {
            if (grid[r][c] == SYM_MINI) miniCount++;
          }
        }
        assertTrue(miniCount <= 1, "Found " + miniCount + " Mini symbols, max is 1");
      }
    }

    @Test
    @DisplayName("H&W grid handles null locked bonuses gracefully")
    void hwNullLockedBonuses() {
      int[][] grid = rngProvider.generateHoldAndWinGrid(config, null);

      assertEquals(3, grid.length);
      assertEquals(5, grid[0].length);
    }
  }

  // ==================== FORCED GRID GENERATION ====================

  @Nested
  @DisplayName("GDD 6/7 - Buy Feature Forced Grid")
  class ForcedGridTests {

    @Test
    @DisplayName("Forced scatter grid has at least 3 scatters on reels 2/3/4")
    void forcedScatterGrid() {
      for (int i = 0; i < 20; i++) {
        int[][] grid = rngProvider.generateForcedScatterGrid(config, 3);
        int scatterCount = 0;
        for (int r = 0; r < 3; r++) {
          for (int c = 0; c < 5; c++) {
            if (grid[r][c] == SYM_SCATTER) scatterCount++;
          }
        }
        assertTrue(scatterCount >= 3, "Expected >= 3 scatters but got " + scatterCount);
      }
    }

    @Test
    @DisplayName("Forced scatter grid places scatters only on cols 1, 2, 3 (reels 2/3/4)")
    void forcedScatterOnCorrectReels() {
      for (int i = 0; i < 20; i++) {
        int[][] grid = rngProvider.generateForcedScatterGrid(config, 3);
        for (int r = 0; r < 3; r++) {
          // Columns 0 and 4 should not have scatter
          assertNotEquals(SYM_SCATTER, grid[r][0], "Scatter should not appear on reel 1 (col 0)");
          assertNotEquals(SYM_SCATTER, grid[r][4], "Scatter should not appear on reel 5 (col 4)");
        }
      }
    }

    @Test
    @DisplayName("Forced bonus grid has at least 6 bonus symbols (GDD 5.2)")
    void forcedBonusGrid() {
      for (int i = 0; i < 20; i++) {
        int[][] grid = rngProvider.generateForcedBonusGrid(config, 6);
        int bonusCount = 0;
        for (int r = 0; r < 3; r++) {
          for (int c = 0; c < 5; c++) {
            int id = grid[r][c];
            if (id == SYM_CASH || id == SYM_MAJOR || id == SYM_MINI) bonusCount++;
          }
        }
        assertTrue(bonusCount >= 6, "Expected >= 6 bonus symbols but got " + bonusCount);
      }
    }
  }

  // ==================== TEST CONFIG BUILDER ====================

  private SlotGameConfig buildTestConfig() {
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

    List<Payline> paylines = List.of(new Payline(1, new int[] {1, 1, 1, 1, 1}));

    // Reel strips that include all symbol types
    List<List<Integer>> reelStrips =
        List.of(
            List.of(
                SYM_A, SYM_B, SYM_C, SYM_D, SYM_E, SYM_F, SYM_G, SYM_H, SYM_WILD, SYM_MAJOR,
                SYM_MINI, SYM_CASH),
            List.of(
                SYM_A,
                SYM_B,
                SYM_C,
                SYM_D,
                SYM_E,
                SYM_F,
                SYM_G,
                SYM_H,
                SYM_SCATTER,
                SYM_WILD,
                SYM_MAJOR,
                SYM_MINI,
                SYM_CASH,
                SYM_STACKED_WILD),
            List.of(
                SYM_A,
                SYM_B,
                SYM_C,
                SYM_D,
                SYM_E,
                SYM_F,
                SYM_G,
                SYM_H,
                SYM_SCATTER,
                SYM_WILD,
                SYM_MAJOR,
                SYM_MINI,
                SYM_CASH,
                SYM_STACKED_WILD),
            List.of(
                SYM_A,
                SYM_B,
                SYM_C,
                SYM_D,
                SYM_E,
                SYM_F,
                SYM_G,
                SYM_H,
                SYM_SCATTER,
                SYM_WILD,
                SYM_MAJOR,
                SYM_MINI,
                SYM_CASH,
                SYM_STACKED_WILD),
            List.of(
                SYM_A,
                SYM_B,
                SYM_C,
                SYM_D,
                SYM_E,
                SYM_F,
                SYM_G,
                SYM_H,
                SYM_WILD,
                SYM_MAJOR,
                SYM_MINI,
                SYM_CASH,
                SYM_STACKED_WILD));

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
        2000.0);
  }
}
