package asia.rgp.game.nagas.modules.slot.domain.service;

import asia.rgp.game.nagas.modules.slot.domain.model.*;
import asia.rgp.game.nagas.shared.domain.model.Matrix;
import asia.rgp.game.nagas.shared.domain.model.Money;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PayoutCalculator {

  private final Random random = new Random();

  /** Calculates the general payout result for a spin. */
  public PayoutResult calculate(
      Matrix matrix, SlotGameConfig config, Money betPerLine, Money totalBet) {
    log.info("=== START PAYOUT CALCULATION ===");
    List<WinDetail> wins = new ArrayList<>();

    // 1. Calculate Line Wins (Base Game)
    if (config.payoutType() == PayoutType.LINE) {
      wins.addAll(calculateAllPaylines(matrix, config, betPerLine));
    }

    // 2. Calculate Scatter Wins
    WinDetail scatterWin = calculateScatterWinDetail(matrix, config, totalBet);
    boolean isFreeSpinTriggered = false;
    int awardedFreeSpins = 0;

    if (scatterWin != null) {
      wins.add(scatterWin);
      log.info(
          "[Scatter Win] Symbol: {}, Count: {}, Amount: {}",
          scatterWin.getSymbolId(),
          scatterWin.getCount(),
          scatterWin.getAmount().getAmount());

      if (scatterWin.getCount() >= config.freeSpinTriggerCount()) {
        isFreeSpinTriggered = true;
        awardedFreeSpins = config.defaultFreeSpinCount();
        log.info(">>> FREE SPINS TRIGGERED: {} spins", awardedFreeSpins);
      }
    }

    // 3. Scan Bonus Symbols (Hold and Win Trigger)
    List<PayoutResult.BonusInfo> bonusInfos = scanBonusSymbols(matrix, config);
    boolean isHoldAndWinTriggered = bonusInfos.size() >= 6;
    if (isHoldAndWinTriggered)
      log.info(">>> HOLD AND WIN TRIGGERED! Bonus count: {}", bonusInfos.size());

    // 4. Scan Glowing Rings (Jackpot Trigger)
    List<int[]> glowingRingPositions = scanGlowingRings(matrix, config);
    boolean isJackpotTriggered = glowingRingPositions.size() >= 6;

    // 5. Aggregate total win
    Money totalWin = wins.stream().map(WinDetail::getAmount).reduce(Money.zero(), Money::plus);

    // 6. Apply Win Cap
    Money winCapLimit = totalBet.times(config.maxWinMultiplier());
    if (totalWin.getAmount() > winCapLimit.getAmount()) {
      log.info(
          "[WinCap] Total win {} exceeds limit {}, capping to limit.",
          totalWin.getAmount(),
          winCapLimit.getAmount());
      totalWin = winCapLimit;
    }

    log.info("=== FINISH PAYOUT CALCULATION. TOTAL WIN: {} ===", totalWin.getAmount());

    return PayoutResult.builder()
        .totalWin(totalWin)
        .wins(wins)
        .triggerFreeSpin(isFreeSpinTriggered)
        .freeSpinCount(awardedFreeSpins)
        .scatterSymbol(config.scatterSymbolId())
        .scatterPositions(scatterWin != null ? scatterWin.getPositions() : new ArrayList<>())
        .triggerHoldAndWin(isHoldAndWinTriggered)
        .bonusInfos(bonusInfos)
        .jackpotTriggered(isJackpotTriggered)
        .glowingRingPositions(glowingRingPositions)
        .build();
  }

  private List<WinDetail> calculateAllPaylines(
      Matrix matrix, SlotGameConfig config, Money betPerLine) {
    List<WinDetail> lineWins = new ArrayList<>();
    for (int i = 0; i < config.paylines().size(); i++) {
      WinDetail win =
          calculateSingleLineWinDetail(matrix, config.paylines().get(i), i + 1, config, betPerLine);
      if (win != null) {
        lineWins.add(win);
      }
    }
    return lineWins;
  }

  private WinDetail calculateSingleLineWinDetail(
      Matrix matrix, Payline line, int lineId, SlotGameConfig config, Money betPerLine) {
    int baseId = -1;
    int match = 0;
    final int WILD_10 = 10;
    final int WILD_14 = 14;

    for (int c = 0; c < matrix.cols(); c++) {
      int row = line.getRowAt(c);
      int id = matrix.getSymbolAt(row, c);
      // Break line sequence if Scatter or Bonus is encountered
      if (id == config.scatterSymbolId() || isBonusSymbol(id)) break;

      if (c == 0) {
        baseId = id;
        match = 1;
        continue;
      }
      boolean isWild = (id == WILD_10 || id == WILD_14 || id == config.wildSymbolId());
      boolean isBaseWild =
          (baseId == WILD_10 || baseId == WILD_14 || baseId == config.wildSymbolId());

      if (id == baseId || isWild) {
        match++;
      } else if (isBaseWild) {
        baseId = id;
        match++;
      } else {
        break;
      }
    }

    SlotSymbol sym = config.symbols().get(baseId);

    if (sym == null) {
      return null;
    }

    double multiplier = sym.getMultiplier(match);
    if (multiplier <= 0) return null;

    Money winAmount = betPerLine.times(multiplier);

    log.info(
        "[Line Win] Line: #{}, Symbol: {}, Match: {}, Multiplier: {}, Win: {}",
        lineId,
        baseId,
        match,
        multiplier,
        winAmount.getAmount());

    List<int[]> pos = new ArrayList<>();
    for (int i = 0; i < match; i++) {
      pos.add(new int[] {i, line.getRowAt(i)});
    }

    return WinDetail.builder()
        .lineId(lineId)
        .symbolId(baseId)
        .count(match)
        .amount(winAmount)
        .positions(pos)
        .type("line")
        .build();
  }

  private WinDetail calculateScatterWinDetail(
      Matrix matrix, SlotGameConfig config, Money totalBet) {
    int count = 0;
    List<int[]> pos = new ArrayList<>();
    int scatterId = config.scatterSymbolId();

    for (int c = 0; c < matrix.cols(); c++) {
      for (int r = 0; r < matrix.rows(); r++) {
        if (matrix.getSymbolAt(r, c) == scatterId) {
          count++;
          pos.add(new int[] {c, r});
        }
      }
    }
    SlotSymbol sym = config.symbols().get(scatterId);
    if (sym == null) return null;

    double multiplier = sym.getMultiplier(count);
    if (multiplier <= 0) return null;

    return WinDetail.builder()
        .symbolId(scatterId)
        .count(count)
        .amount(totalBet.times(multiplier))
        .positions(pos)
        .type("scatter")
        .build();
  }

  private List<PayoutResult.BonusInfo> scanBonusSymbols(Matrix matrix, SlotGameConfig config) {
    List<PayoutResult.BonusInfo> list = new ArrayList<>();
    for (int c = 0; c < matrix.cols(); c++) {
      for (int r = 0; r < matrix.rows(); r++) {
        int id = matrix.getSymbolAt(r, c);
        if (isBonusSymbol(id)) {
          double mult;
          String type;
          if (id == SlotConstants.SYMBOL_MAJOR) {
            mult = 100.0;
            type = SlotConstants.TYPE_MAJOR;
          } else if (id == SlotConstants.SYMBOL_MINI) {
            mult = 25.0;
            type = SlotConstants.TYPE_MINI;
          } else {
            mult = generateRandomCashMultiplier();
            type = SlotConstants.TYPE_CASH;
          }
          list.add(
              PayoutResult.BonusInfo.builder()
                  .row(r)
                  .col(c)
                  .symbolId(id)
                  .multiplier(mult)
                  .type(type)
                  .build());
        }
      }
    }
    return list;
  }

  private List<int[]> scanGlowingRings(Matrix matrix, SlotGameConfig config) {
    List<int[]> positions = new ArrayList<>();
    for (int c = 0; c < matrix.cols(); c++) {
      for (int r = 0; r < matrix.rows(); r++) {
        int symbolId = matrix.getSymbolAt(r, c);
        if (isJackpotEligible(symbolId, config)) {
          if (matrix.hasOverlayAt(r, c, "GLOWING_RING")) {
            positions.add(new int[] {c, r});
          }
        }
      }
    }
    return positions;
  }

  private boolean isJackpotEligible(int id, SlotGameConfig config) {
    return id != config.scatterSymbolId()
        && id != SlotConstants.SYMBOL_STACKED_WILD
        && !isBonusSymbol(id);
  }

  private boolean isBonusSymbol(int id) {
    return id == SlotConstants.SYMBOL_CASH
        || id == SlotConstants.SYMBOL_MAJOR
        || id == SlotConstants.SYMBOL_MINI;
  }

  public double generateRandomCashMultiplier() {
    double[] opts = {1.0, 2.0, 3.0, 4.0, 5.0, 7.0, 10.0, 12.0, 15.0, 18.0, 20.0};
    return opts[random.nextInt(opts.length)];
  }
}
