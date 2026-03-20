package asia.rgp.game.nagas.modules.slot.domain.model;

import asia.rgp.game.nagas.shared.domain.model.Money;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

/**
 * [DOMAIN MODEL]
 */
@Getter
@Builder
public class WinDetail {
    private final int lineId;
    private final int symbolId;
    private final int count;
    private final Money amount;
    private final List<int[]> positions;
    private final List<Integer> matchingSymbols;
    private final String type;
}