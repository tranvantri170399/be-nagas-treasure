package asia.rgp.game.nagas;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class NagasApplicationTest {

  @Test
  void applicationCanBeInstantiated() {
    NagasApplication app = new NagasApplication();
    assertNotNull(app, "NagasApplication should instantiate");
  }
}
