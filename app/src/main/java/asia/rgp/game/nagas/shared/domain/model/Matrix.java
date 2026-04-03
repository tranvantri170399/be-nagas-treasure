package asia.rgp.game.nagas.shared.domain.model;

import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.error.ErrorCode;
import java.io.Serializable;
import java.util.Arrays;

/**
 * [SHARED KERNEL] Represents the visual grid of a slot game result. Supports symbol data and
 * overlay layers (e.g., Glowing Rings).
 */
public final class Matrix implements Serializable {
  private static final long serialVersionUID = 1L;

  private final int rows;
  private final int cols;
  private final int[][] grid;

  /* Overlay layer for Naga Glowing Rings [GDD 8.2] */
  private final boolean[][] glowingRings;

  public Matrix(int rows, int cols, int[][] grid) {
    validate(rows, cols, grid);
    this.rows = rows;
    this.cols = cols;
    this.grid = deepCopy(grid);
    this.glowingRings = new boolean[rows][cols]; // Default all false
  }

  /* Logic to set overlay, usually called by RngProvider based on Bet Tier [GDD 8.2] */
  public void setOverlayAt(int row, int col, String type, boolean active) {
    if ("GLOWING_RING".equals(type)) {
      this.glowingRings[row][col] = active;
    }
  }

  /**
   * Checks if a specific cell has an overlay (e.g., Naga Glowing Ring). This fixes the undefined
   * method error in PayoutCalculator.
   */
  public boolean hasOverlayAt(int row, int col, String type) {
    if ("GLOWING_RING".equals(type)) {
      return glowingRings[row][col];
    }
    return false;
  }

  public int getSymbolAt(int row, int col) {
    return grid[row][col];
  }

  public int[][] getGrid() {
    return deepCopy(this.grid);
  }

  private void validate(int rows, int cols, int[][] grid) {
    if (grid == null || grid.length != rows) {
      throw new DomainException("Matrix rows mismatch", ErrorCode.SPIN_RESULT_REEL_ROWS_INVALID);
    }
    for (int[] r : grid) {
      if (r == null || r.length != cols) {
        throw new DomainException(
            "Matrix columns mismatch", ErrorCode.SPIN_RESULT_REELS_SIZE_MISMATCH);
      }
    }
  }

  private int[][] deepCopy(int[][] original) {
    return Arrays.stream(original).map(int[]::clone).toArray(int[][]::new);
  }

  public int rows() {
    return rows;
  }

  public int cols() {
    return cols;
  }
}
