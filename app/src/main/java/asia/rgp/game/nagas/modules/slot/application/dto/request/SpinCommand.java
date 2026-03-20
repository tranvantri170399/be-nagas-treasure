package asia.rgp.game.nagas.modules.slot.application.dto.request;

import lombok.Getter;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import asia.rgp.game.nagas.shared.domain.model.Money;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpinCommand {
    private String userId; 
    private String gameId;
    private Money betAmount;
    private String sessionId; 
}