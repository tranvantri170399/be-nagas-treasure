package asia.rgp.game.nagas.infrastructure.debug;

import asia.rgp.game.nagas.infrastructure.cache.HotCacheService;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotState;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.adapter.SlotStateRepository;
import asia.rgp.game.nagas.shared.domain.model.Money;
import java.time.Duration;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Stores cheat flags in Redis with 5-minute TTL. Each cheat applies to the NEXT spin only, then
 * auto-clears. Production builds exclude this bean entirely via @Profile.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile({"dev", "staging"})
public class CheatService {

  private final HotCacheService cache;
  private final SlotStateRepository stateRepository;

  private static final String CHEAT_KEY_PREFIX = "cheat:";
  private static final String JACKPOT_POOL_PREFIX = "jackpot:pools:";
  private static final Duration CHEAT_TTL = Duration.ofMinutes(5);

  // ============================================================
  // CHEAT STORAGE (Redis with TTL)
  // ============================================================

  /** Store a cheat that will be consumed by the next spin. */
  public void setCheat(String agencyId, String userId, CheatCode code, Map<String, Object> value) {
    String key = cheatKey(agencyId, userId);
    Map<String, Object> cheatData = new HashMap<>();
    cheatData.put("code", code.name());
    cheatData.put("value", value != null ? value : Map.of());
    cache.put(key, cheatData, CHEAT_TTL);
    log.warn(
        "[CHEAT] SET | agent={} | user={} | code={} | value={}", agencyId, userId, code, value);
  }

  /** Read and consume the active cheat for a user. Returns null if no cheat is active. */
  @SuppressWarnings("unchecked")
  public ActiveCheat consumeCheat(String agencyId, String userId) {
    String key = cheatKey(agencyId, userId);
    Optional<Map> raw = cache.get(key, Map.class);
    if (raw.isEmpty()) return null;

    cache.evict(key); // Auto-clear after consumption
    Map<String, Object> data = raw.get();
    String codeName = (String) data.get("code");
    Map<String, Object> value = (Map<String, Object>) data.getOrDefault("value", Map.of());

    log.warn("[CHEAT] CONSUMED | agent={} | user={} | code={}", agencyId, userId, codeName);
    return new ActiveCheat(CheatCode.valueOf(codeName), value);
  }

  // ============================================================
  // IMMEDIATE STATE MUTATIONS
  // ============================================================

  /** Set jackpot pool tiers to specific values. */
  public void setJackpotPool(String agencyId, Map<String, Double> pools) {
    pools.forEach(
        (tier, amount) ->
            cache.putHash(JACKPOT_POOL_PREFIX + agencyId, tier, String.valueOf(amount)));
    log.warn("[CHEAT] SET_JACKPOT_POOL | agent={} | pools={}", agencyId, pools);
  }

  /** Reset jackpot pools to seed values. */
  public void resetJackpotPool(String agencyId) {
    Map<String, Double> seeds =
        Map.of(
            SlotConstants.JACKPOT_DIAMOND, 10000.0,
            SlotConstants.JACKPOT_RUBY, 500.0,
            SlotConstants.JACKPOT_EMERALD, 50.0,
            SlotConstants.JACKPOT_SAPPHIRE, 10.0);
    setJackpotPool(agencyId, seeds);
    log.warn("[CHEAT] RESET_JACKPOT_POOL | agent={}", agencyId);
  }

  /** Set player to a specific game mode by manipulating state directly. */
  public void setGameMode(String agencyId, String userId, String gameId, String mode, Money bet) {
    switch (mode) {
      case SlotConstants.MODE_FREE -> {
        SlotState fs =
            SlotState.builder()
                .agencyId(agencyId)
                .userId(userId)
                .gameId(gameId)
                .sessionId("cheat-session")
                .totalFreeSpins(8)
                .remainingFreeSpins(8)
                .baseBet(bet)
                .freeSpinMode(true)
                .holdAndWin(false)
                .parentRoundId(UUID.randomUUID().toString())
                .baseRoundNumber(1)
                .accumulatedWin(0.0)
                .build();
        stateRepository.save(fs);
      }
      case SlotConstants.MODE_HOLD_AND_WIN -> {
        List<SlotState.LockedBonus> locked = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
          locked.add(new SlotState.LockedBonus(i / 5, i % 5, 13, 5.0, "CASH"));
        }
        SlotState hw =
            SlotState.builder()
                .agencyId(agencyId)
                .userId(userId)
                .gameId(gameId)
                .sessionId("cheat-session")
                .holdAndWin(true)
                .freeSpinMode(false)
                .remainingRespins(3)
                .lockedBonuses(locked)
                .baseBet(bet)
                .parentRoundId(UUID.randomUUID().toString())
                .baseRoundNumber(1)
                .accumulatedWin(0.0)
                .build();
        stateRepository.save(hw);
      }
      case SlotConstants.MODE_BASE -> stateRepository.delete(agencyId, userId, gameId);
      default -> log.warn("[CHEAT] Unknown mode: {}", mode);
    }
    log.warn("[CHEAT] SET_GAME_MODE | agent={} | user={} | mode={}", agencyId, userId, mode);
  }

  /** Set remaining free spin count. */
  public void setFreeSpinCount(String agencyId, String userId, String gameId, int count) {
    stateRepository
        .find(agencyId, userId, gameId)
        .ifPresent(
            state -> {
              state.setRemainingFreeSpins(count);
              state.setTotalFreeSpins(Math.max(state.getTotalFreeSpins(), count));
              state.setFreeSpinMode(count > 0);
              stateRepository.save(state);
            });
    log.warn(
        "[CHEAT] SET_FREE_SPIN_COUNT | agent={} | user={} | count={}", agencyId, userId, count);
  }

  /** Set accumulated win value. */
  public void setAccumulatedWin(String agencyId, String userId, String gameId, double amount) {
    stateRepository
        .find(agencyId, userId, gameId)
        .ifPresent(
            state -> {
              state.setAccumulatedWin(amount);
              stateRepository.save(state);
            });
    log.warn(
        "[CHEAT] SET_ACCUMULATED_WIN | agent={} | user={} | amount={}", agencyId, userId, amount);
  }

  /** Clear all game state for an agent. */
  public void clearAgentState(String agencyId, String userId, String gameId) {
    stateRepository.delete(agencyId, userId, gameId);
    log.warn("[CHEAT] CLEAR_STATE | agent={} | user={}", agencyId, userId);
  }

  /** Reset everything for an agent: state + jackpot pools. */
  public void resetSession(String agencyId, String userId, String gameId) {
    stateRepository.delete(agencyId, userId, gameId);
    resetJackpotPool(agencyId);
    log.warn("[CHEAT] RESET_SESSION | agent={} | user={}", agencyId, userId);
  }

  private String cheatKey(String agencyId, String userId) {
    return CHEAT_KEY_PREFIX + agencyId + ":" + userId;
  }

  /** Immutable cheat data consumed by SpinUseCaseImpl. */
  public record ActiveCheat(CheatCode code, Map<String, Object> value) {}
}
