package asia.rgp.game.nagas.shared.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MatrixTest {

  @Nested
  @DisplayName("GDD 2.1 - Grid Construction (5x3)")
  class ConstructionTests {

    @Test
    @DisplayName("Creates 3x5 matrix matching GDD grid (5 reels, 3 rows)")
    void validGridCreation() {
      int[][] grid = {
        {1, 2, 3, 4, 5},
        {6, 7, 8, 1, 2},
        {3, 4, 5, 6, 7}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      assertEquals(3, matrix.rows());
      assertEquals(5, matrix.cols());
    }

    @Test
    @DisplayName("getSymbolAt returns correct symbol")
    void getSymbolAt() {
      int[][] grid = {
        {1, 2, 3, 4, 5},
        {6, 7, 8, 9, 10},
        {11, 12, 13, 1, 2}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      assertEquals(1, matrix.getSymbolAt(0, 0));
      assertEquals(10, matrix.getSymbolAt(1, 4));
      assertEquals(13, matrix.getSymbolAt(2, 2));
    }

    @Test
    @DisplayName("Grid is deep-copied (immutability)")
    void gridDeepCopy() {
      int[][] grid = {
        {1, 2, 3, 4, 5},
        {6, 7, 8, 9, 10},
        {11, 12, 13, 1, 2}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      // Modify original grid
      grid[0][0] = 999;

      // Matrix should be unaffected
      assertEquals(1, matrix.getSymbolAt(0, 0));
    }

    @Test
    @DisplayName("getGrid() returns a copy, not the internal array")
    void getGridReturnsCopy() {
      int[][] grid = {
        {1, 2, 3, 4, 5},
        {6, 7, 8, 9, 10},
        {11, 12, 13, 1, 2}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      int[][] copy = matrix.getGrid();
      copy[0][0] = 999;

      assertEquals(1, matrix.getSymbolAt(0, 0));
    }

    @Test
    @DisplayName("Invalid row count throws DomainException")
    void invalidRows() {
      int[][] grid = {
        {1, 2, 3, 4, 5},
        {6, 7, 8, 9, 10}
      };
      assertThrows(DomainException.class, () -> new Matrix(3, 5, grid));
    }

    @Test
    @DisplayName("Mismatched column count throws DomainException")
    void mismatchedCols() {
      int[][] grid = {
        {1, 2, 3, 4},
        {6, 7, 8, 9, 10},
        {11, 12, 13, 1, 2}
      };
      assertThrows(DomainException.class, () -> new Matrix(3, 5, grid));
    }

    @Test
    @DisplayName("Null grid throws DomainException")
    void nullGrid() {
      assertThrows(DomainException.class, () -> new Matrix(3, 5, null));
    }
  }

  @Nested
  @DisplayName("GDD 8.2 - Glowing Ring Overlay")
  class OverlayTests {

    @Test
    @DisplayName("Glowing ring overlay defaults to false")
    void defaultNoOverlay() {
      int[][] grid = {
        {1, 2, 3, 4, 5},
        {6, 7, 8, 9, 10},
        {11, 12, 13, 1, 2}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      for (int r = 0; r < 3; r++) {
        for (int c = 0; c < 5; c++) {
          assertFalse(matrix.hasOverlayAt(r, c, "GLOWING_RING"));
        }
      }
    }

    @Test
    @DisplayName("Set and check glowing ring overlay")
    void setAndCheckOverlay() {
      int[][] grid = {
        {1, 2, 3, 4, 5},
        {6, 7, 8, 9, 10},
        {11, 12, 13, 1, 2}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      matrix.setOverlayAt(1, 3, "GLOWING_RING", true);

      assertTrue(matrix.hasOverlayAt(1, 3, "GLOWING_RING"));
      assertFalse(matrix.hasOverlayAt(0, 0, "GLOWING_RING"));
    }

    @Test
    @DisplayName("Unknown overlay type returns false")
    void unknownOverlayType() {
      int[][] grid = {
        {1, 2, 3, 4, 5},
        {6, 7, 8, 9, 10},
        {11, 12, 13, 1, 2}
      };
      Matrix matrix = new Matrix(3, 5, grid);

      assertFalse(matrix.hasOverlayAt(0, 0, "UNKNOWN_TYPE"));
    }
  }
}
