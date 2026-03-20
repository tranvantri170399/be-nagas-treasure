package asia.rgp.game.nagas.shared.domain.model;

import java.io.Serializable;
import java.util.Arrays;

import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.error.ErrorCode;

/**
 * [SHARED KERNEL] Represents the visual grid of a slot game result.
 */
public final class Matrix implements Serializable {
    private final int rows;
    private final int cols;
    private final int[][] grid;

    public Matrix(int rows, int cols, int[][] grid) {
        validate(rows, cols, grid);
        this.rows = rows;
        this.cols = cols;
        this.grid = deepCopy(grid);
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
                throw new DomainException("Matrix columns mismatch", ErrorCode.SPIN_RESULT_REELS_SIZE_MISMATCH);
            }
        }
    }

    public int getSymbolAt(int row, int col) {
        return grid[row][col];
    }

    private int[][] deepCopy(int[][] original) {
        return Arrays.stream(original).map(int[]::clone).toArray(int[][]::new);
    }

    public int rows() { return rows; }
    public int cols() { return cols; }
}