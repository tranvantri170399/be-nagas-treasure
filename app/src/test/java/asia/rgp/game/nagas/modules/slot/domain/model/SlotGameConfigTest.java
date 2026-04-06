package asia.rgp.game.nagas.modules.slot.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SlotGameConfigTest {

  @Nested
  @DisplayName("GDD 2.1 - Config Validation")
  class ValidationTests {

    @Test
    @DisplayName("Valid config creates successfully")
    void validConfig() {
      SlotGameConfig config = buildMinimalConfig();
      assertEquals("test", config.gameId());
      assertEquals(3, config.rows());
      assertEquals(5, config.cols());
    }

    @Test
    @DisplayName("Blank gameId throws DomainException")
    void blankGameIdThrows() {
      assertThrows(DomainException.class, () -> buildConfigWithGameId(""));
    }

    @Test
    @DisplayName("Null gameId throws DomainException")
    void nullGameIdThrows() {
      assertThrows(DomainException.class, () -> buildConfigWithGameId(null));
    }

    @Test
    @DisplayName("Reel strip count must match columns")
    void reelStripCountMismatch() {
      assertThrows(
          DomainException.class,
          () ->
              new SlotGameConfig(
                  "test",
                  3,
                  5,
                  PayoutType.LINE,
                  List.of(List.of(1), List.of(1), List.of(1)), // Only 3, need 5
                  List.of(new Payline(1, new int[] {1, 1, 1, 1, 1})),
                  buildSymbols(),
                  9,
                  10,
                  13,
                  11,
                  12,
                  3,
                  8,
                  2000.0));
    }

    @Test
    @DisplayName("LINE payout type requires paylines")
    void lineTypeRequiresPaylines() {
      assertThrows(
          DomainException.class,
          () ->
              new SlotGameConfig(
                  "test",
                  3,
                  5,
                  PayoutType.LINE,
                  buildReelStrips(),
                  List.of(), // Empty paylines
                  buildSymbols(),
                  9,
                  10,
                  13,
                  11,
                  12,
                  3,
                  8,
                  2000.0));
    }

    @Test
    @DisplayName("Missing symbol definition throws DomainException")
    void missingSymbolThrows() {
      Map<Integer, SlotSymbol> incompleteSymbols =
          Map.of(
              1, new SlotSymbol(1, "A", Map.of(3, 0.2), false, false),
              9, new SlotSymbol(9, "SCATTER", Map.of(3, 2.0), false, true),
              10, new SlotSymbol(10, "WILD", Map.of(), true, false));

      assertThrows(
          DomainException.class,
          () ->
              new SlotGameConfig(
                  "test",
                  3,
                  5,
                  PayoutType.LINE,
                  buildReelStrips(),
                  List.of(new Payline(1, new int[] {1, 1, 1, 1, 1})),
                  incompleteSymbols,
                  9,
                  10,
                  13, // Bonus (13) not in map
                  11, // Major (11) not in map
                  12, // Mini (12) not in map
                  3,
                  8,
                  2000.0));
    }
  }

  @Nested
  @DisplayName("GDD Config Properties")
  class PropertyTests {

    @Test
    @DisplayName("Free spin trigger count is 3 (GDD 4.2)")
    void freeSpinTriggerCount() {
      SlotGameConfig config = buildMinimalConfig();
      assertEquals(3, config.freeSpinTriggerCount());
    }

    @Test
    @DisplayName("Default free spin count is 8 (GDD 4.3)")
    void defaultFreeSpinCount() {
      SlotGameConfig config = buildMinimalConfig();
      assertEquals(8, config.defaultFreeSpinCount());
    }

    @Test
    @DisplayName("Max win multiplier is 2000 (GDD 2.3)")
    void maxWinMultiplier() {
      SlotGameConfig config = buildMinimalConfig();
      assertEquals(2000.0, config.maxWinMultiplier(), 0.001);
    }

    @Test
    @DisplayName("hasPaylines() returns true for LINE type with paylines")
    void hasPaylines() {
      SlotGameConfig config = buildMinimalConfig();
      assertTrue(config.hasPaylines());
    }

    @Test
    @DisplayName("getSymbol() returns correct symbol")
    void getSymbol() {
      SlotGameConfig config = buildMinimalConfig();
      SlotSymbol scatter = config.getSymbol(9);
      assertEquals("SCATTER", scatter.name());
      assertTrue(scatter.isScatter());
    }

    @Test
    @DisplayName("getSymbol() throws for unknown symbol ID")
    void getSymbolUnknownThrows() {
      SlotGameConfig config = buildMinimalConfig();
      assertThrows(DomainException.class, () -> config.getSymbol(99));
    }

    @Test
    @DisplayName("Reel strips are immutable (deep copy)")
    void reelStripsImmutable() {
      SlotGameConfig config = buildMinimalConfig();
      assertThrows(UnsupportedOperationException.class, () -> config.reelStrips().add(List.of(1)));
    }
  }

  // ==================== HELPERS ====================

  private SlotGameConfig buildMinimalConfig() {
    return new SlotGameConfig(
        "test",
        3,
        5,
        PayoutType.LINE,
        buildReelStrips(),
        List.of(new Payline(1, new int[] {1, 1, 1, 1, 1})),
        buildSymbols(),
        9,
        10,
        13,
        11,
        12,
        3,
        8,
        2000.0);
  }

  private SlotGameConfig buildConfigWithGameId(String gameId) {
    return new SlotGameConfig(
        gameId,
        3,
        5,
        PayoutType.LINE,
        buildReelStrips(),
        List.of(new Payline(1, new int[] {1, 1, 1, 1, 1})),
        buildSymbols(),
        9,
        10,
        13,
        11,
        12,
        3,
        8,
        2000.0);
  }

  private Map<Integer, SlotSymbol> buildSymbols() {
    return Map.ofEntries(
        Map.entry(1, new SlotSymbol(1, "A", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
        Map.entry(2, new SlotSymbol(2, "B", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
        Map.entry(3, new SlotSymbol(3, "C", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
        Map.entry(4, new SlotSymbol(4, "D", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
        Map.entry(5, new SlotSymbol(5, "E", Map.of(3, 0.4, 4, 1.0, 5, 4.0), false, false)),
        Map.entry(6, new SlotSymbol(6, "F", Map.of(3, 0.4, 4, 2.0, 5, 6.0), false, false)),
        Map.entry(7, new SlotSymbol(7, "G", Map.of(3, 0.4, 4, 3.0, 5, 8.0), false, false)),
        Map.entry(8, new SlotSymbol(8, "H", Map.of(3, 1.0, 4, 4.0, 5, 10.0), false, false)),
        Map.entry(9, new SlotSymbol(9, "SCATTER", Map.of(3, 2.0), false, true)),
        Map.entry(10, new SlotSymbol(10, "WILD", Map.of(), true, false)),
        Map.entry(11, new SlotSymbol(11, "MAJOR", Map.of(), false, false)),
        Map.entry(12, new SlotSymbol(12, "MINI", Map.of(), false, false)),
        Map.entry(13, new SlotSymbol(13, "BONUS", Map.of(), false, false)),
        Map.entry(14, new SlotSymbol(14, "STACKED_WILD", Map.of(), true, false)));
  }

  private List<List<Integer>> buildReelStrips() {
    return List.of(
        List.of(1, 2, 3, 4, 5, 6, 7, 8, 10),
        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 13),
        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 13),
        List.of(1, 2, 3, 4, 5, 6, 7, 8, 10, 13));
  }
}
