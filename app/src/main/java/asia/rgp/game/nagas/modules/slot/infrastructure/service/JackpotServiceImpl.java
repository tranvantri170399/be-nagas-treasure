package asia.rgp.game.nagas.modules.slot.infrastructure.service;

import asia.rgp.game.nagas.infrastructure.cache.HotCacheService;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.modules.slot.domain.service.JackpotService;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity.JackpotAuditEntity;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.repository.MongoJackpotAuditRepository;
import asia.rgp.game.nagas.shared.domain.model.Money;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JackpotServiceImpl implements JackpotService {

  private final HotCacheService hotCacheService;
  private final MongoJackpotAuditRepository auditRepository;
  private final SecureRandom random = new SecureRandom();

  private static final String JACKPOT_CACHE_KEY_PREFIX = "jackpot:pools:";
  private static final String STATUS_CLAIMED = "CLAIMED";
  private static final String STATUS_PAID = "PAID";
  private static final String STATUS_FAILED = "FAILED";

  private static final Map<String, Double> SEED_VALUES =
      Map.of(
          SlotConstants.JACKPOT_DIAMOND, 10000.0,
          SlotConstants.JACKPOT_RUBY, 500.0,
          SlotConstants.JACKPOT_EMERALD, 50.0,
          SlotConstants.JACKPOT_SAPPHIRE, 10.0);

  private String poolKey(String agencyId) {
    return JACKPOT_CACHE_KEY_PREFIX + agencyId;
  }

  public void initPoolsIfAbsent(String agencyId) {
    String key = poolKey(agencyId);
    SEED_VALUES.forEach(
        (tier, seedAmount) -> {
          Object current = hotCacheService.getHash(key, tier);
          boolean needsInit = false;

          if (current == null) {
            needsInit = true;
          } else {
            try {
              double currentVal = Double.parseDouble(current.toString());
              if (currentVal < seedAmount) needsInit = true;
            } catch (Exception e) {
              needsInit = true;
            }
          }

          if (needsInit) {
            log.info("[JACKPOT] Init seed for agent={}, tier={}: {}", agencyId, tier, seedAmount);
            hotCacheService.putHash(key, tier, String.valueOf(seedAmount));
          }
        });
  }

  @Override
  public void contribute(String agencyId, Money amount) {
    String key = poolKey(agencyId);
    double totalAmount = amount.getAmount();
    hotCacheService.incrementHash(key, SlotConstants.JACKPOT_DIAMOND, round4(totalAmount * 0.005));
    hotCacheService.incrementHash(key, SlotConstants.JACKPOT_RUBY, round4(totalAmount * 0.008));
    hotCacheService.incrementHash(key, SlotConstants.JACKPOT_EMERALD, round4(totalAmount * 0.01));
    hotCacheService.incrementHash(key, SlotConstants.JACKPOT_SAPPHIRE, round4(totalAmount * 0.017));
  }

  /**
   * Atomically claims a jackpot pool and records the win.
   *
   * <p>Flow: 1. Determine winning tier via RNG 2. Atomic Lua: read pool amount AND reset to seed in
   * one operation (no race window) 3. Save audit record with status=CLAIMED 4. Return result —
   * caller credits wallet then calls markPaid()
   */
  @Override
  public JackpotSpinResult spinWheel(
      String agencyId, String userId, String sessionId, Money currentBet) {
    // --- Step 1: Determine tier ---
    double betFactor = currentBet.getAmount();
    double roll = random.nextDouble();

    String wonTier;
    boolean hitArrow = false;
    boolean isNearMiss = false;

    if (roll < 0.0001 * betFactor) {
      wonTier = SlotConstants.JACKPOT_DIAMOND;
      hitArrow = true;
    } else if (roll < 0.005 * betFactor) {
      wonTier = SlotConstants.JACKPOT_RUBY;
      hitArrow = true;
    } else if (roll < 0.15) {
      wonTier = SlotConstants.JACKPOT_EMERALD;
      if (random.nextDouble() < 0.20) {
        hitArrow = true;
        isNearMiss = true;
      }
    } else {
      wonTier = SlotConstants.JACKPOT_SAPPHIRE;
      if (random.nextDouble() < 0.05) {
        hitArrow = true;
        isNearMiss = true;
      }
    }

    // --- Step 2: Atomic claim — read current pool AND reset to seed in one Lua call ---
    String key = poolKey(agencyId);
    double seed = SEED_VALUES.getOrDefault(wonTier, 0.0);
    String claimedValueStr = hotCacheService.getAndResetHash(key, wonTier, String.valueOf(seed));

    double prize;
    if (claimedValueStr == null) {
      prize = seed;
    } else {
      try {
        prize = Math.max(Double.parseDouble(claimedValueStr), seed);
      } catch (NumberFormatException e) {
        prize = seed;
      }
    }

    String winId = UUID.randomUUID().toString();

    // --- Step 3: Save audit record with CLAIMED status ---
    JackpotAuditEntity audit =
        JackpotAuditEntity.builder()
            .id(UUID.randomUUID().toString())
            .winId(winId)
            .agencyId(agencyId)
            .userId(userId)
            .sessionId(sessionId)
            .tier(wonTier)
            .amount(prize)
            .poolBefore(prize)
            .poolAfterReset(seed)
            .status(STATUS_CLAIMED)
            .hitArrow(hitArrow)
            .isNearMiss(isNearMiss)
            .createdAt(Instant.now())
            .build();

    try {
      auditRepository.save(audit);
    } catch (Exception e) {
      log.error(
          "[JACKPOT-AUDIT] Failed to save CLAIMED audit for winId={}: {}", winId, e.getMessage());
    }

    log.info(
        "[JACKPOT] CLAIMED | winId={} | agent={} | user={} | tier={} | prize={} | poolBefore={} | resetTo={}",
        winId,
        agencyId,
        userId,
        wonTier,
        prize,
        prize,
        seed);

    return JackpotSpinResult.builder()
        .winId(winId)
        .tierName(wonTier)
        .amount(Money.of(prize))
        .hitArrow(hitArrow)
        .nearMiss(isNearMiss)
        .build();
  }

  /**
   * Marks a jackpot win as PAID after wallet credit succeeds. Uses optimistic locking (@Version) —
   * if concurrent update is attempted, OptimisticLockingFailureException is thrown.
   */
  public void markPaid(String winId) {
    auditRepository
        .findByWinId(winId)
        .ifPresent(
            audit -> {
              if (STATUS_CLAIMED.equals(audit.getStatus())) {
                audit.setStatus(STATUS_PAID);
                audit.setPaidAt(Instant.now());
                auditRepository.save(audit);
                log.info("[JACKPOT-AUDIT] PAID | winId={}", winId);
              }
            });
  }

  /**
   * Marks a jackpot win as FAILED if wallet credit fails. The pool was already reset — this records
   * the failure for manual reconciliation.
   */
  public void markFailed(String winId, String errorMessage) {
    auditRepository
        .findByWinId(winId)
        .ifPresent(
            audit -> {
              if (STATUS_CLAIMED.equals(audit.getStatus())) {
                audit.setStatus(STATUS_FAILED);
                audit.setErrorMessage(errorMessage);
                auditRepository.save(audit);
                log.error("[JACKPOT-AUDIT] FAILED | winId={} | error={}", winId, errorMessage);
              }
            });
  }

  @Override
  public Map<String, Double> getAllPools(String agencyId) {
    String key = poolKey(agencyId);
    Map<String, Double> pools = new HashMap<>();
    SEED_VALUES.keySet().forEach(tier -> pools.put(tier, getCurrentPoolAmount(key, tier)));
    return pools;
  }

  private double getCurrentPoolAmount(String key, String tier) {
    Object val = hotCacheService.getHash(key, tier);
    double seed = SEED_VALUES.getOrDefault(tier, 0.0);
    if (val == null) return seed;
    try {
      return Math.max(Double.parseDouble(val.toString()), seed);
    } catch (Exception e) {
      return seed;
    }
  }

  private double round4(double value) {
    return Math.round(value * 10000.0) / 10000.0;
  }
}
