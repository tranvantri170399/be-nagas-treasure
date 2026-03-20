package asia.rgp.game.nagas.modules.slot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record SlotSymbol(
    int id, 
    String name, 
    @JsonProperty("payTable") Map<Integer, Double> payTable, 
    @JsonProperty("isWild") boolean isWild,
    @JsonProperty("isScatter") boolean isScatter
) {
    public double getMultiplier(int matchCount) {
        if (payTable == null) {
            return 0.0;
        }
        return payTable.getOrDefault(matchCount, 0.0);
    }

    public boolean canSubstitute() {
        return isWild;
    }
}