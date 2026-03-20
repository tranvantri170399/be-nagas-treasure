package asia.rgp.game.nagas.infrastructure.grpc;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum defining all supported WS Proxy command codes.
 */
public enum PluginCommand {

    JOIN(1005, "Connect and join game"),
    SPIN(1500, "Place bet and spin"),
    BUY_FEATURE(1501, "Buy free games feature"),
    LAST_SESSION(1502, "Get last session state"),
    GET_BALANCE(1503, "Get user balance");

    private final int code;
    private final String description;

    PluginCommand(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Find PluginCommand by numeric code.
     *
     * @param code the command code
     * @return Optional containing the matching command, or empty if not found
     */
    public static Optional<PluginCommand> fromCode(int code) {
        return Arrays.stream(values())
                .filter(cmd -> cmd.code == code)
                .findFirst();
    }
}
