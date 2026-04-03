package asia.rgp.game.nagas.modules.slot.domain.model;

import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.error.ErrorCode;

/**
 * [DOMAIN MODEL] Defines a winning pattern across the matrix. The 'positions' array stores the row
 * index for each column. Example: [1, 1, 1, 1, 1] for a 5-column slot means the middle row across
 * all reels.
 */
public record Payline(int id, int[] positions) {

  public Payline {
    if (positions == null || positions.length == 0) {
      throw new DomainException(
          "Payline positions cannot be empty", ErrorCode.INVALID_PAYLINE_CONFIG);
    }
  }

  /**
   * Gets the target row index for a specific column on this payline.
   *
   * @param column The reel index (0 to N)
   * @return The row index defined for this payline at that column.
   */
  public int getRowAt(int column) {
    if (column < 0 || column >= positions.length) {
      throw new DomainException("Column index out of payline bounds", ErrorCode.DOMAIN_ERROR);
    }
    return positions[column];
  }

  /** Returns the number of columns covered by this payline. */
  public int length() {
    return positions.length;
  }
}
