package asia.rgp.game.nagas.modules.slot.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.error.ErrorCode;

/**
 * [DOMAIN MODEL] Holds the entire configuration for a specific slot game.
 */
public record SlotGameConfig(
    String gameId,
    int rows,
    int cols,
    PayoutType payoutType,
    List<List<Integer>> reelStrips,
    List<Payline> paylines,
    Map<Integer, SlotSymbol> symbols,

    int scatterSymbolId,
    int wildSymbolId,
    int bonusSymbolId,
    int majorSymbolId,
    int miniSymbolId,
    int freeSpinTriggerCount, 
    int defaultFreeSpinCount, 
    double maxWinMultiplier   
) {
    /**
     * Canonical constructor
     */
    public SlotGameConfig {
        if (gameId == null || gameId.isBlank()) {
            throw new DomainException("Game ID cannot be empty", ErrorCode.INVALID_ARGUMENT);
        }

        if (reelStrips == null || reelStrips.size() != cols) {
            throw new DomainException(
                "Reel strips count (%d) must match columns count (%d)".formatted(
                    reelStrips != null ? reelStrips.size() : 0, cols), 
                ErrorCode.INVALID_DEFAULT_REEL_STRIP_COUNT
            );
        }

        if (payoutType == PayoutType.LINE && (paylines == null || paylines.isEmpty())) {
            throw new DomainException("Line-based games must have at least one payline", ErrorCode.INVALID_PAYLINE_CONFIG);
        }

        if (symbols == null || symbols.isEmpty()) {
            throw new DomainException("Symbol configuration cannot be empty", ErrorCode.SYMBOL_CONFIG_EMPTY);
        }

        validateSymbolId(symbols, scatterSymbolId, "Scatter");
        validateSymbolId(symbols, wildSymbolId, "Wild");
        validateSymbolId(symbols, bonusSymbolId, "Bonus/Cash");
        validateSymbolId(symbols, majorSymbolId, "Major");
        validateSymbolId(symbols, miniSymbolId, "Mini");

        // Data Protection
        reelStrips = List.copyOf(reelStrips.stream().map(List::copyOf).toList());
        paylines = (paylines != null) ? List.copyOf(paylines) : Collections.emptyList();
        symbols = Map.copyOf(symbols);
    }

    private void validateSymbolId(Map<Integer, SlotSymbol> symbols, int id, String name) {
        if (!symbols.containsKey(id)) {
            throw new DomainException(name + " Symbol ID " + id + " not defined in symbols map", ErrorCode.SYMBOL_NOT_FOUND);
        }
    }

    public SlotSymbol getSymbol(int symbolId) {
        SlotSymbol symbol = symbols.get(symbolId);
        if (symbol == null) {
            throw new DomainException("Symbol ID " + symbolId + " not found in config", ErrorCode.SYMBOL_NOT_FOUND);
        }
        return symbol;
    }

    public int getStripSize(int colIndex) {
        if (colIndex < 0 || colIndex >= reelStrips.size()) {
            throw new DomainException("Invalid reel index: " + colIndex, ErrorCode.DOMAIN_ERROR);
        }
        return reelStrips.get(colIndex).size();
    }

    public boolean hasPaylines() {
        return payoutType == PayoutType.LINE && !paylines.isEmpty();
    }
}