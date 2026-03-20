package asia.rgp.game.nagas.shared.infrastructure.rng;

import org.springframework.stereotype.Component;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotGameConfig;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotState;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class RngProvider {
    private final SecureRandom random = new SecureRandom();

    private static final int SYMBOL_BLANK = 0;

    public int nextInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    public int[][] generateHoldAndWinGrid(SlotGameConfig config, List<SlotState.LockedBonus> lockedBonuses) {
        int rows = config.rows();
        int cols = config.cols();
        int[][] grid = new int[rows][cols];

        int cashId = config.bonusSymbolId();
        int majorId = config.majorSymbolId();
        int miniId = config.miniSymbolId();

        if (lockedBonuses != null) {
            for (SlotState.LockedBonus locked : lockedBonuses) {
                int symId = getSymbolIdFromType(locked.getType(), cashId, majorId, miniId);
                grid[locked.getRow()][locked.getCol()] = symId;
            }
        }

        double hitRate = 0.0; 
        if (this.random.nextDouble() < hitRate) {
            List<int[]> emptyCells = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (grid[r][c] == SYMBOL_BLANK) emptyCells.add(new int[]{r, c});
                }
            }

            if (!emptyCells.isEmpty()) {
                Collections.shuffle(emptyCells, random);
                int[] pos = emptyCells.get(0);
                
                double typeRoll = this.random.nextDouble();
                if (typeRoll < 0.03) grid[pos[0]][pos[1]] = majorId;
                else if (typeRoll < 0.10) grid[pos[0]][pos[1]] = miniId;
                else grid[pos[0]][pos[1]] = cashId;
            }
        }

        return grid;
    }

    public int[][] generateForcedBonusGrid(SlotGameConfig config, int minBonus) {
        int[][] grid = generateGridFromStrips(config);
        int cashId = config.bonusSymbolId();
        int majorId = config.majorSymbolId();
        int miniId = config.miniSymbolId();
        
        List<int[]> emptyCells = new ArrayList<>();
        int currentBonusCount = 0;

        for (int r = 0; r < config.rows(); r++) {
            for (int c = 0; c < config.cols(); c++) {
                int id = grid[r][r];
                if (id == cashId || id == majorId || id == miniId) {
                    currentBonusCount++;
                } else if (grid[r][c] != config.scatterSymbolId()) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }

        if (currentBonusCount < minBonus) {
            Collections.shuffle(emptyCells, random);
            int needed = minBonus - currentBonusCount;
            for (int i = 0; i < Math.min(needed, emptyCells.size()); i++) {
                int[] pos = emptyCells.get(i);
                grid[pos[0]][pos[1]] = cashId;
            }
        }
        return grid;
    }

    private int getSymbolIdFromType(String type, int cashId, int majorId, int miniId) {
        if (type == null) return cashId;
        return switch (type.toUpperCase()) {
            case "MAJOR" -> majorId;
            case "MINI" -> miniId;
            default -> cashId;
        };
    }

    public int[][] generateGridFromStrips(SlotGameConfig config) {
        int rows = config.rows();
        int cols = config.cols();
        int[][] grid = new int[rows][cols];
        for (int col = 0; col < cols; col++) {
            List<Integer> strip = config.reelStrips().get(col);
            int stopPosition = this.random.nextInt(strip.size());
            for (int row = 0; row < rows; row++) {
                grid[row][col] = strip.get((stopPosition + row) % strip.size());
            }
        }
        return grid;
    }

    public int[][] generateForcedScatterGrid(SlotGameConfig config, int minScatter) {
        int[][] grid = generateGridFromStrips(config);
        int scatterId = config.scatterSymbolId();
        int currentCount = 0;
        for (int r = 0; r < config.rows(); r++) {
            for (int c = 0; c < config.cols(); c++) {
                if (grid[r][c] == scatterId) currentCount++;
            }
        }
        if (currentCount < minScatter) {
            int needed = minScatter - currentCount;
            for (int c = 0; c < config.cols() && needed > 0; c++) {
                if (!hasScatterInColumn(grid, c, scatterId)) {
                    grid[random.nextInt(config.rows())][c] = scatterId;
                    needed--;
                }
            }
        }
        return grid;
    }

    private boolean hasScatterInColumn(int[][] grid, int col, int scatterId) {
        for (int[] row : grid) {
            if (row[col] == scatterId) return true;
        }
        return false;
    }
}