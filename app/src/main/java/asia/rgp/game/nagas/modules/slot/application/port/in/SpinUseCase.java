package asia.rgp.game.nagas.modules.slot.application.port.in;

import asia.rgp.game.nagas.modules.slot.application.dto.request.BuyFeatureCommand;
import asia.rgp.game.nagas.modules.slot.application.dto.request.SpinCommand;
import asia.rgp.game.nagas.modules.slot.presentation.dto.response.SlotResultResponse;

public interface SpinUseCase {
    SlotResultResponse execute(SpinCommand command);

    SlotResultResponse executeBuyFeature(BuyFeatureCommand command);

    SlotResultResponse executeBuyHoldAndWin(BuyFeatureCommand command);
}