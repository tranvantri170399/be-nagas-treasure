package asia.rgp.game.nagas.modules.slot.application.port.out;

import asia.rgp.game.nagas.modules.slot.domain.model.SlotHistory;

public interface SlotHistoryPort {
    void save(SlotHistory history);
}