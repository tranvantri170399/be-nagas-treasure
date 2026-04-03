package asia.rgp.game.nagas.shared.infrastructure.exception;

import asia.rgp.game.nagas.shared.presentation.api.ErrorConstant;

/** Exception thrown when an entity is not found in the database. */
public class EntityNotFoundException extends DatabaseException {

  public EntityNotFoundException(String message) {
    super(message, ErrorConstant.ENTITY_NOT_FOUND);
  }

  public EntityNotFoundException(String message, Throwable cause) {
    super(message, ErrorConstant.ENTITY_NOT_FOUND, cause);
  }

  public EntityNotFoundException(String entityName, Object id) {
    super(String.format("%s with id %s not found", entityName, id), ErrorConstant.ENTITY_NOT_FOUND);
  }
}
