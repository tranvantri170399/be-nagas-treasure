package asia.rgp.game.nagas.modules.slot.application.port.out;

import asia.rgp.game.nagas.modules.slot.domain.model.SlotGameConfig;
import java.util.Optional;

/**
 * [OUTPUT PORT] Interface for retrieving slot game configurations. This abstraction allows the
 * application to remain agnostic of the data source (e.g., Local JSON, Database, or Remote Config
 * Service).
 */
public interface GameConfigPort {

  /**
   * Finds the configuration for a specific game by its ID. * @param gameId The unique identifier of
   * the game (e.g., "slot_777").
   *
   * @return An Optional containing the SlotGameConfig if found, otherwise empty.
   */
  Optional<SlotGameConfig> findByGameId(String gameId);
}
