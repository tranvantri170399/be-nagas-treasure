package asia.rgp.game.nagas.modules.slot.application.usecase;

import asia.rgp.game.nagas.modules.slot.domain.model.*;
import java.util.List;
import java.util.Map;

/** Shared test config builder replicating the production nagas_treasure.json config. */
final class TestConfigBuilder {

  private TestConfigBuilder() {}

  @SuppressWarnings("all")
  static SlotGameConfig buildFullConfig(String gameId) {
    Map<Integer, SlotSymbol> symbols =
        Map.ofEntries(
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

    List<Payline> paylines =
        List.of(
            new Payline(1, new int[] {1, 1, 1, 1, 1}),
            new Payline(2, new int[] {0, 0, 0, 0, 0}),
            new Payline(3, new int[] {2, 2, 2, 2, 2}),
            new Payline(4, new int[] {0, 1, 2, 1, 0}),
            new Payline(5, new int[] {2, 1, 0, 1, 2}),
            new Payline(6, new int[] {1, 0, 0, 0, 1}),
            new Payline(7, new int[] {1, 2, 2, 2, 1}),
            new Payline(8, new int[] {0, 0, 1, 2, 2}),
            new Payline(9, new int[] {2, 2, 1, 0, 0}),
            new Payline(10, new int[] {1, 2, 1, 0, 1}),
            new Payline(11, new int[] {1, 0, 1, 2, 1}),
            new Payline(12, new int[] {0, 1, 1, 1, 0}),
            new Payline(13, new int[] {2, 1, 1, 1, 2}),
            new Payline(14, new int[] {0, 1, 0, 1, 0}),
            new Payline(15, new int[] {2, 1, 2, 1, 2}),
            new Payline(16, new int[] {1, 1, 0, 1, 1}),
            new Payline(17, new int[] {1, 1, 2, 1, 1}),
            new Payline(18, new int[] {0, 0, 2, 0, 0}),
            new Payline(19, new int[] {2, 2, 0, 2, 2}),
            new Payline(20, new int[] {0, 2, 2, 2, 0}),
            new Payline(21, new int[] {2, 0, 0, 0, 2}),
            new Payline(22, new int[] {1, 2, 0, 2, 1}),
            new Payline(23, new int[] {1, 0, 2, 0, 1}),
            new Payline(24, new int[] {0, 2, 0, 2, 0}),
            new Payline(25, new int[] {2, 0, 2, 0, 2}));

    List<List<Integer>> reelStrips =
        List.of(
            List.of(
                1, 1, 2, 2, 3, 3, 5, 6, 10, 1, 1, 2, 2, 3, 3, 13, 7, 8, 1, 1, 2, 2, 3, 3, 10, 5, 6,
                11, 12, 1, 1, 2, 2, 3, 3),
            List.of(
                1, 1, 2, 2, 3, 3, 10, 6, 13, 1, 1, 2, 2, 3, 3, 14, 8, 5, 11, 12, 1, 1, 2, 2, 3, 3,
                7, 9, 4, 10, 1, 1, 2, 2, 3, 3),
            List.of(
                1, 1, 2, 2, 3, 3, 5, 13, 11, 12, 10, 1, 1, 2, 2, 3, 3, 14, 7, 13, 6, 1, 1, 2, 2, 3,
                3, 8, 13, 9, 10, 1, 1, 2, 2, 3, 3),
            List.of(
                1, 1, 2, 2, 3, 3, 13, 5, 10, 1, 1, 2, 2, 3, 3, 7, 9, 11, 12, 14, 4, 10, 1, 1, 2, 2,
                3, 3, 13, 8, 1, 1, 2, 2, 3, 3),
            List.of(
                1, 1, 2, 2, 3, 3, 13, 1, 10, 7, 13, 1, 1, 2, 2, 3, 3, 11, 12, 14, 6, 13, 10, 1, 1,
                2, 2, 3, 3, 13, 4, 8, 1, 1, 2, 2, 3, 3));

    return new SlotGameConfig(
        gameId,
        3,
        5,
        PayoutType.LINE,
        reelStrips,
        paylines,
        symbols,
        9,
        10,
        13,
        11,
        12,
        3,
        8,
        2000.0);
  }
}
