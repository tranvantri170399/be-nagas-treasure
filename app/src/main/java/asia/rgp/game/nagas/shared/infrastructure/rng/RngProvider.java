package asia.rgp.game.nagas.shared.infrastructure.rng;

import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotGameConfig;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotState;
import asia.rgp.game.nagas.shared.domain.model.Matrix;
import asia.rgp.game.nagas.shared.domain.model.Money;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RngProvider {
  private final SecureRandom random = new SecureRandom();

  public Matrix generateMatrix(SlotGameConfig config, Money currentBet, boolean isFreeSpin) {
    int[][] grid = generateGridFromStrips(config, isFreeSpin);

    applyStackedWildExpansion(grid, config);

    Matrix matrix = new Matrix(config.rows(), config.cols(), grid);

    applyJackpotOverlays(matrix, config, currentBet);

    return matrix;
  }

  public int[][] generateGridFromStrips(SlotGameConfig config, boolean isFreeSpin) {
    int rows = config.rows();
    int cols = config.cols();
    int[][] grid = new int[rows][cols];

    for (int col = 0; col < cols; col++) {
      List<Integer> strip = config.reelStrips().get(col);
      if (isFreeSpin) {
        strip =
            strip.stream()
                .filter(
                    id ->
                        id != SlotConstants.SYMBOL_A
                            && id != SlotConstants.SYMBOL_B
                            && id != SlotConstants.SYMBOL_C
                            && id != SlotConstants.SYMBOL_D
                            && id != SlotConstants.SYMBOL_MAJOR
                            && id != SlotConstants.SYMBOL_MINI)
                .collect(Collectors.toList());
      }

      int stopPosition = this.random.nextInt(strip.size());
      for (int row = 0; row < rows; row++) {
        grid[row][col] = strip.get((stopPosition + row) % strip.size());
      }
    }
    return grid;
  }

  public void applyStackedWildExpansion(int[][] grid, SlotGameConfig config) {
    int rows = config.rows();
    int cols = config.cols();
    int STACKED_WILD_ID = 14;
    int NORMAL_WILD_ID = 10;

    for (int c = 0; c < cols; c++) {
      boolean hasStackedWild = false;
      for (int r = 0; r < rows; r++) {
        if (grid[r][c] == STACKED_WILD_ID) {
          hasStackedWild = true;
          break;
        }
      }
      if (hasStackedWild) {
        for (int r = 0; r < rows; r++) {
          grid[r][c] = NORMAL_WILD_ID;
        }
      }
    }
  }

  public int[][] generateHoldAndWinGrid(
      SlotGameConfig config, List<SlotState.LockedBonus> lockedBonuses) {
    int rows = config.rows();
    int cols = config.cols();
    int[][] grid = new int[rows][cols];

    boolean hasMajor = false;
    boolean hasMini = false;

    if (lockedBonuses != null) {
      for (SlotState.LockedBonus locked : lockedBonuses) {
        grid[locked.getRow()][locked.getCol()] = locked.getSymbolId();
        if (locked.getSymbolId() == config.majorSymbolId()) hasMajor = true;
        if (locked.getSymbolId() == config.miniSymbolId()) hasMini = true;
      }
    }
    if (this.random.nextDouble() < 0.15) {
      List<int[]> emptyCells = new ArrayList<>();
      for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
          if (grid[r][c] == 0) emptyCells.add(new int[] {r, c});
        }
      }

      if (!emptyCells.isEmpty()) {
        int[] pos = emptyCells.get(this.random.nextInt(emptyCells.size()));
        double typeRoll = this.random.nextDouble();
        int newSymbolId;
        if (typeRoll < 0.03 && !hasMajor) newSymbolId = config.majorSymbolId();
        else if (typeRoll < 0.10 && !hasMini) newSymbolId = config.miniSymbolId();
        else newSymbolId = config.bonusSymbolId();

        grid[pos[0]][pos[1]] = newSymbolId;
      }
    }
    return grid;
  }

  public int[][] generateForcedBonusGrid(SlotGameConfig config, int minBonus) {
    int[][] grid = generateGridFromStrips(config, false);
    int cashId = config.bonusSymbolId();
    List<int[]> nonBonusCells = new ArrayList<>();
    int currentBonusCount = 0;

    for (int r = 0; r < config.rows(); r++) {
      for (int c = 0; c < config.cols(); c++) {
        if (isBonusSymbol(grid[r][c])) currentBonusCount++;
        else nonBonusCells.add(new int[] {r, c});
      }
    }

    if (currentBonusCount < minBonus) {
      Collections.shuffle(nonBonusCells, random);
      for (int i = 0; i < (minBonus - currentBonusCount) && i < nonBonusCells.size(); i++) {
        int[] pos = nonBonusCells.get(i);
        grid[pos[0]][pos[1]] = cashId;
      }
    }
    return grid;
  }

  public int[][] generateForcedScatterGrid(SlotGameConfig config, int minScatter) {
    int[][] grid = generateGridFromStrips(config, false);
    int scatterId = config.scatterSymbolId();
    for (int r = 0; r < config.rows(); r++) {
      for (int c = 0; c < config.cols(); c++) {
        if (grid[r][c] == scatterId) grid[r][c] = 1;
      }
    }

    List<Integer> targetCols = new ArrayList<>(List.of(1, 2, 3));
    Collections.shuffle(targetCols, random);

    for (int i = 0; i < minScatter; i++) {
      int col = targetCols.get(i % targetCols.size());
      int row = random.nextInt(config.rows());
      grid[row][col] = scatterId;
    }
    return grid;
  }

  public void applyJackpotOverlays(Matrix matrix, SlotGameConfig config, Money currentBet) {
    double cellProbability = 0.12;
    double betFactor = Math.max(1.0, currentBet.getAmount() / 1000.0);
    double finalProb = Math.min(0.25, cellProbability * betFactor);

    int ringCount = 0;
    for (int r = 0; r < matrix.rows(); r++) {
      for (int c = 0; c < matrix.cols(); c++) {
        int symbolId = matrix.getSymbolAt(r, c);
        if (isNormalSymbol(symbolId, config)) {
          if (this.random.nextDouble() < finalProb) {
            matrix.setOverlayAt(r, c, "GLOWING_RING", true);
            ringCount++;
          }
        }
      }
    }

    if (ringCount == 4 && this.random.nextDouble() < 0.3) {
      forceAddOneRing(matrix, config);
    }
  }

  /** Force at least {@code minRings} glowing rings on eligible cells. Used by cheat system. */
  public void forceJackpotOverlays(Matrix matrix, SlotGameConfig config, int minRings) {
    int ringCount = 0;
    for (int r = 0; r < matrix.rows(); r++) {
      for (int c = 0; c < matrix.cols(); c++) {
        if (matrix.hasOverlayAt(r, c, "GLOWING_RING")) ringCount++;
      }
    }
    for (int r = 0; r < matrix.rows() && ringCount < minRings; r++) {
      for (int c = 0; c < matrix.cols() && ringCount < minRings; c++) {
        int id = matrix.getSymbolAt(r, c);
        if (isNormalSymbol(id, config) && !matrix.hasOverlayAt(r, c, "GLOWING_RING")) {
          matrix.setOverlayAt(r, c, "GLOWING_RING", true);
          ringCount++;
        }
      }
    }
  }

  private void forceAddOneRing(Matrix matrix, SlotGameConfig config) {
    for (int r = 0; r < matrix.rows(); r++) {
      for (int c = 0; c < matrix.cols(); c++) {
        int id = matrix.getSymbolAt(r, c);
        if (isNormalSymbol(id, config) && !matrix.hasOverlayAt(r, c, "GLOWING_RING")) {
          matrix.setOverlayAt(r, c, "GLOWING_RING", true);
          return;
        }
      }
    }
  }

  private boolean isNormalSymbol(int id, SlotGameConfig config) {
    return id != config.scatterSymbolId() && !isBonusSymbol(id);
  }

  private boolean isBonusSymbol(int id) {
    return id == SlotConstants.SYMBOL_CASH
        || id == SlotConstants.SYMBOL_MAJOR
        || id == SlotConstants.SYMBOL_MINI;
  }
}
