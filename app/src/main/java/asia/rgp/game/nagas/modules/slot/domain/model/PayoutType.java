package asia.rgp.game.nagas.modules.slot.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PayoutType {
  LINE("LINE"),
  WAY("WAY"),
  CLUSTER("CLUSTER");

  private final String code;

  public static PayoutType fromCode(String code) {
    for (PayoutType type : PayoutType.values()) {
      if (type.code.equalsIgnoreCase(code)) {
        return type;
      }
    }
    return LINE;
  }
}
