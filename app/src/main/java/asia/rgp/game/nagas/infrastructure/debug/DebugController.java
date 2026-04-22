package asia.rgp.game.nagas.infrastructure.debug;

import asia.rgp.game.nagas.shared.domain.model.Money;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Debug/cheat API for FE integration testing. Completely absent from production builds —
 * the @Profile annotation ensures Spring never creates this bean when profile != dev/staging.
 */
@Slf4j
@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Profile({"dev", "staging"})
public class DebugController {

  private final CheatService cheatService;
  private static final String DEBUG_TOKEN = "nagas-debug-2024";

  // ============================================================
  // CHEAT ENDPOINTS
  // ============================================================

  /**
   * Apply a cheat for the next spin. The cheat auto-clears after one spin or after 5 minutes.
   *
   * <p>Example: POST /debug/cheat/agent-1/player-1 Body: {"cheat": "FORCE_FREE_SPIN", "value": {}}
   */
  @PostMapping("/cheat/{agencyId}/{userId}")
  public ResponseEntity<?> applyCheat(
      @PathVariable String agencyId,
      @PathVariable String userId,
      @RequestHeader(value = "X-Debug-Token", required = false) String token,
      @RequestBody Map<String, Object> body) {

    if (!DEBUG_TOKEN.equals(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "Invalid debug token"));
    }

    String cheatName = (String) body.get("cheat");
    if (cheatName == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Missing 'cheat' field"));
    }

    CheatCode code;
    try {
      code = CheatCode.valueOf(cheatName);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Unknown cheat: " + cheatName, "valid", CheatCode.values()));
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> value = (Map<String, Object>) body.getOrDefault("value", Map.of());

    // Handle immediate mutations vs next-spin cheats
    String description = handleCheat(agencyId, userId, code, value);

    log.warn(
        "[DEBUG-API] cheat={} | agent={} | user={} | at={}",
        cheatName,
        agencyId,
        userId,
        Instant.now());

    return ResponseEntity.ok(
        Map.of(
            "applied", true,
            "cheat", cheatName,
            "description", description,
            "agencyId", agencyId,
            "userId", userId,
            "expiresIn", "5 minutes (or next spin)"));
  }

  /**
   * Set jackpot pool values directly.
   *
   * <p>Example: POST /debug/jackpot/agent-1 Body: {"DIAMOND": 100.0, "RUBY": 50.0}
   */
  @PostMapping("/jackpot/{agencyId}")
  public ResponseEntity<?> setJackpotPool(
      @PathVariable String agencyId,
      @RequestHeader(value = "X-Debug-Token", required = false) String token,
      @RequestBody Map<String, Double> pools) {

    if (!DEBUG_TOKEN.equals(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "Invalid debug token"));
    }

    cheatService.setJackpotPool(agencyId, pools);
    return ResponseEntity.ok(Map.of("applied", true, "agencyId", agencyId, "pools", pools));
  }

  /**
   * Set player session state directly.
   *
   * <p>Example: POST /debug/state/agent-1/player-1 Body: {"game_id": "nagas_treasure", "mode":
   * "free", "bet": 1.0}
   */
  @PostMapping("/state/{agencyId}/{userId}")
  public ResponseEntity<?> setState(
      @PathVariable String agencyId,
      @PathVariable String userId,
      @RequestHeader(value = "X-Debug-Token", required = false) String token,
      @RequestBody Map<String, Object> body) {

    if (!DEBUG_TOKEN.equals(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "Invalid debug token"));
    }

    String gameId = (String) body.getOrDefault("game_id", "nagas_treasure");
    String mode = (String) body.getOrDefault("mode", "base");
    double bet = ((Number) body.getOrDefault("bet", 1.0)).doubleValue();

    cheatService.setGameMode(agencyId, userId, gameId, mode, Money.of(bet));
    return ResponseEntity.ok(
        Map.of("applied", true, "agencyId", agencyId, "userId", userId, "mode", mode));
  }

  /** Reset all state for an agent+user: game state + jackpot pools. */
  @DeleteMapping("/state/{agencyId}/{userId}")
  public ResponseEntity<?> resetState(
      @PathVariable String agencyId,
      @PathVariable String userId,
      @RequestHeader(value = "X-Debug-Token", required = false) String token,
      @RequestParam(defaultValue = "nagas_treasure") String gameId) {

    if (!DEBUG_TOKEN.equals(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "Invalid debug token"));
    }

    cheatService.resetSession(agencyId, userId, gameId);
    return ResponseEntity.ok(
        Map.of("applied", true, "reset", "session + jackpot pools", "agencyId", agencyId));
  }

  /** List all available cheat codes. */
  @GetMapping("/cheats")
  public ResponseEntity<?> listCheats(
      @RequestHeader(value = "X-Debug-Token", required = false) String token) {
    if (!DEBUG_TOKEN.equals(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "Invalid debug token"));
    }
    Map<String, String> nextSpin = new LinkedHashMap<>();
    nextSpin.put("FORCE_FREE_SPIN", "Next spin triggers Free Spin (3 scatters forced)");
    nextSpin.put("FORCE_HOLD_AND_WIN", "Next spin triggers Hold & Win (6+ bonuses forced)");
    nextSpin.put("FORCE_JACKPOT", "Next base spin triggers jackpot wheel");
    nextSpin.put("FORCE_NORMAL_WIN", "Next spin uses normal RNG (no force)");
    nextSpin.put("FORCE_LOSS", "Next spin grid is all blanks/low symbols");
    nextSpin.put("FORCE_WIN_CAP", "Force win to hit 2000x cap");
    nextSpin.put("FORCE_GRID", "value: {grid: [[1,2,3,4,5],[...],[...]]}");
    nextSpin.put("FORCE_HW_LOCKED_COUNT", "value: {count: 10}");
    nextSpin.put("FORCE_HW_IN_FREE_SPIN", "Next FS spin triggers H&W inside free spins");
    nextSpin.put("FORCE_LAST_FREE_SPIN", "Set remaining FS to 1");
    nextSpin.put("FORCE_JACKPOT_TRIGGER", "Force jackpot trigger on next eligible spin");

    Map<String, String> immediate = new LinkedHashMap<>();
    immediate.put("SET_JACKPOT_POOL", "value: {DIAMOND: 100.0, RUBY: 10.0}");
    immediate.put("RESET_JACKPOT_POOL", "Reset all pools to seed values");
    immediate.put("SET_FREE_SPIN_COUNT", "value: {count: 3, game_id: nagas_treasure}");
    immediate.put("SET_GAME_MODE", "value: {mode: free, game_id: nagas_treasure}");
    immediate.put("SET_ACCUMULATED_WIN", "value: {amount: 100.5, game_id: nagas_treasure}");
    immediate.put("RESET_SESSION", "value: {game_id: nagas_treasure}");
    immediate.put("CLEAR_AGENT_STATE", "value: {game_id: nagas_treasure}");

    return ResponseEntity.ok(Map.of("nextSpinCheats", nextSpin, "immediateCheats", immediate));
  }

  // ============================================================
  // INTERNAL
  // ============================================================

  @SuppressWarnings("unchecked")
  private String handleCheat(
      String agencyId, String userId, CheatCode code, Map<String, Object> value) {
    return switch (code) {
      // --- Immediate state mutations ---
      case SET_JACKPOT_POOL -> {
        cheatService.setJackpotPool(agencyId, toDoubleMap(value));
        yield "Jackpot pools updated immediately";
      }
      case RESET_JACKPOT_POOL -> {
        cheatService.resetJackpotPool(agencyId);
        yield "Jackpot pools reset to seed values";
      }
      case SET_FREE_SPIN_COUNT -> {
        String gid = (String) value.getOrDefault("game_id", "nagas_treasure");
        int count = ((Number) value.getOrDefault("count", 1)).intValue();
        cheatService.setFreeSpinCount(agencyId, userId, gid, count);
        yield "Free spin count set to " + count;
      }
      case SET_GAME_MODE -> {
        String gid = (String) value.getOrDefault("game_id", "nagas_treasure");
        String mode = (String) value.getOrDefault("mode", "base");
        cheatService.setGameMode(agencyId, userId, gid, mode, Money.of(1.0));
        yield "Game mode set to " + mode;
      }
      case SET_ACCUMULATED_WIN -> {
        String gid = (String) value.getOrDefault("game_id", "nagas_treasure");
        double amt = ((Number) value.getOrDefault("amount", 0.0)).doubleValue();
        cheatService.setAccumulatedWin(agencyId, userId, gid, amt);
        yield "Accumulated win set to " + amt;
      }
      case RESET_SESSION -> {
        String gid = (String) value.getOrDefault("game_id", "nagas_treasure");
        cheatService.resetSession(agencyId, userId, gid);
        yield "Session and jackpot pools reset";
      }
      case CLEAR_AGENT_STATE -> {
        String gid = (String) value.getOrDefault("game_id", "nagas_treasure");
        cheatService.clearAgentState(agencyId, userId, gid);
        yield "Agent state cleared";
      }
      case FORCE_LAST_FREE_SPIN -> {
        String gid = (String) value.getOrDefault("game_id", "nagas_treasure");
        cheatService.setFreeSpinCount(agencyId, userId, gid, 1);
        yield "Remaining free spins set to 1 (last spin)";
      }
      // --- Next-spin cheats (stored in Redis, consumed on next spin) ---
      default -> {
        cheatService.setCheat(agencyId, userId, code, value);
        yield "Will apply on next spin: " + code.name();
      }
    };
  }

  private Map<String, Double> toDoubleMap(Map<String, Object> input) {
    Map<String, Double> result = new java.util.HashMap<>();
    input.forEach(
        (k, v) -> {
          if (v instanceof Number n) result.put(k, n.doubleValue());
        });
    return result;
  }
}
