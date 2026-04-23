package asia.rgp.game.nagas.infrastructure.grpc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for building and parsing ZMQ topic URN strings.
 *
 * <p>Format: urn:ws:z:{zoneId}:r:{roomId}:p:{pluginId}:u:{userId}:s:{sessionId}
 *
 * <p>Compatible with WS Proxy KeyUtils pattern.
 */
public final class ZmqTopicHelper {

  // Matches any pair like "z:value", "r:value", etc.
  private static final Pattern PAIR_PATTERN = Pattern.compile("(?<tag>[zrpusi]):(?<val>[^:]+)");

  private ZmqTopicHelper() {
    // Utility class
  }

  public record WSKey(
      Optional<String> urn,
      Optional<String> zoneId,
      Optional<String> roomId,
      Optional<String> pluginId,
      Optional<String> userId,
      Optional<String> sessionId) {}

  /**
   * Parse any URN-like string by scanning for known tags. Works with "urn:ws:z:1:s:sess_abc" OR
   * "z:1:s:sess_abc" OR "s:sess_abc:z:1"
   */
  public static WSKey parse(String urn) {
    if (urn == null) {
      return new WSKey(
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty());
    }

    Map<String, String> found = new HashMap<>();
    Matcher matcher = PAIR_PATTERN.matcher(urn);

    while (matcher.find()) {
      found.put(matcher.group("tag"), matcher.group("val"));
    }

    return new WSKey(
        Optional.ofNullable(found.get("urn")),
        Optional.ofNullable(found.get("z")),
        Optional.ofNullable(found.get("r")),
        Optional.ofNullable(found.get("p")),
        Optional.ofNullable(found.get("u")),
        Optional.ofNullable(found.get("s")));
  }

  /**
   * Flexible builder: only includes what is provided.
   *
   * @param urn URN prefix (e.g. "ws")
   * @param zoneId zone identifier
   * @param roomId room identifier
   * @param pluginId plugin identifier
   * @param userId user identifier
   * @param sessionId session identifier
   * @return formatted URN string
   */
  public static String build(
      String urn, String zoneId, String roomId, String pluginId, String userId, String sessionId) {
    StringJoiner joiner = new StringJoiner(":");

    if (urn != null) joiner.add("urn").add(urn);
    if (zoneId != null) joiner.add("z").add(zoneId);
    if (roomId != null) joiner.add("r").add(roomId);
    if (pluginId != null) joiner.add("p").add(pluginId);
    if (userId != null) joiner.add("u").add(userId);
    else if (sessionId != null) joiner.add("s").add(sessionId);

    return joiner.toString();
  }

  /**
   * Shorthand: build topic for a user session (most common usage). Produces:
   * urn:ws:z:{zone}:s:{sessionId}
   */
  public static String buildTopic(String zone, String sessionId) {
    return build("ws", normalizeZone(zone), null, null, null, sessionId);
  }

  private static String normalizeZone(String zone) {
    if (zone == null || zone.isBlank()) {
      return zone;
    }

    String normalized = zone.replace("GatewayZone", "");
    return normalized.isBlank() ? zone : normalized;
  }
}
