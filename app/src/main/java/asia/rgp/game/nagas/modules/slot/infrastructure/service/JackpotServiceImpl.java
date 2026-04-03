package asia.rgp.game.nagas.modules.slot.infrastructure.service;

import asia.rgp.game.nagas.infrastructure.cache.HotCacheService;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.modules.slot.domain.service.JackpotService;
import asia.rgp.game.nagas.shared.domain.model.Money;
import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JackpotServiceImpl implements JackpotService {

  private final HotCacheService hotCacheService;
  private final SecureRandom random = new SecureRandom();

  private static final String JACKPOT_CACHE_KEY = "jackpot:pools";

  // Seed values
  private static final Map<String, Double> SEED_VALUES =
      Map.of(
          SlotConstants.JACKPOT_DIAMOND, 10000.0,
          SlotConstants.JACKPOT_RUBY, 500.0,
          SlotConstants.JACKPOT_EMERALD, 50.0,
          SlotConstants.JACKPOT_SAPPHIRE, 10.0);

  @PostConstruct
  public void initPoolsIfAbsent() {
    SEED_VALUES.forEach(
        (tier, seedAmount) -> {
          Object current = hotCacheService.getHash(JACKPOT_CACHE_KEY, tier);
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
            log.info("[JACKPOT] Force initializing seed for {}: {}", tier, seedAmount);
            hotCacheService.putHash(JACKPOT_CACHE_KEY, tier, String.valueOf(seedAmount));
          }
        });
  }

  @Override
  public void contribute(Money amount) {
    double totalAmount = amount.getAmount();
    updatePool(SlotConstants.JACKPOT_DIAMOND, totalAmount * (0.5 / 100.0));
    updatePool(SlotConstants.JACKPOT_RUBY, totalAmount * (0.8 / 100.0));
    updatePool(SlotConstants.JACKPOT_EMERALD, totalAmount * (1.0 / 100.0));
    updatePool(SlotConstants.JACKPOT_SAPPHIRE, totalAmount * (1.7 / 100.0));
  }

  @Override
  public JackpotSpinResult spinWheel(Money currentBet) {
    double betFactor = currentBet.getAmount() / 1.0;
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

    double prize = getCurrentPoolAmount(wonTier);
    resetPool(wonTier);

    log.info("[JACKPOT] WINNER! Tier: {} | Prize: {} | Arrow: {}", wonTier, prize, hitArrow);

    return new JackpotSpinResult(wonTier, Money.of(prize), hitArrow, isNearMiss);
  }

  private void updatePool(String tier, double value) {
    double roundedValue = Math.round(value * 10000.0) / 10000.0;
    hotCacheService.incrementHash(JACKPOT_CACHE_KEY, tier, roundedValue);
  }

  private double getCurrentPoolAmount(String tier) {
    Object val = hotCacheService.getHash(JACKPOT_CACHE_KEY, tier);
    double seed = SEED_VALUES.getOrDefault(tier, 0.0);

    if (val == null) return seed;
    try {
      double amount = Double.parseDouble(val.toString());
      return Math.max(amount, seed);
    } catch (Exception e) {
      return seed;
    }
  }

  private void resetPool(String tier) {
    double seed = SEED_VALUES.getOrDefault(tier, 0.0);
    hotCacheService.putHash(JACKPOT_CACHE_KEY, tier, String.valueOf(seed));
  }

  @Override
  public Map<String, Double> getAllPools() {
    Map<String, Double> pools = new HashMap<>();
    SEED_VALUES
        .keySet()
        .forEach(
            tier -> {
              pools.put(tier, getCurrentPoolAmount(tier));
            });
    return pools;
  }
}
