package asia.rgp.game.nagas;

import org.junit.jupiter.api.Test;

import asia.rgp.game.nagas.NagasApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NagasApplicationTest {

    @Test
    void applicationCanBeInstantiated() {
        NagasApplication app = new NagasApplication();
        assertNotNull(app, "NagasApplication should instantiate");
    }
}

