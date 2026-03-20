package asia.rgp.game.nagas.modules.slot.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import asia.rgp.game.nagas.modules.slot.domain.model.*;
import asia.rgp.game.nagas.shared.domain.model.Matrix;
import asia.rgp.game.nagas.shared.domain.model.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class PayoutCalculator {

    private final Random random = new Random();

    public PayoutResult calculate(Matrix matrix, SlotGameConfig config, Money betPerLine, Money totalBet) {
        List<WinDetail> wins = new ArrayList<>();
        
        if (config.payoutType() == PayoutType.LINE) {
            wins.addAll(calculateAllPaylines(matrix, config, betPerLine));
        }

        WinDetail scatterWin = calculateScatterWinDetail(matrix, config, totalBet);
        boolean isFreeSpinTriggered = false;
        int awardedFreeSpins = 0;

        if (scatterWin != null) {
            wins.add(scatterWin);
            if (scatterWin.getCount() >= config.freeSpinTriggerCount()) {
                isFreeSpinTriggered = true;
                awardedFreeSpins = config.defaultFreeSpinCount();
            }
        }

        List<PayoutResult.BonusInfo> bonusInfos = scanBonusSymbols(matrix, config);
        boolean isHoldAndWinTriggered = bonusInfos.size() >= 6;

        Money totalWin = wins.stream()
                .map(WinDetail::getAmount)
                .reduce(Money.zero(), Money::plus);

        Money winCapLimit = totalBet.times(config.maxWinMultiplier());
        if (totalWin.getAmount() > winCapLimit.getAmount()) {
            totalWin = winCapLimit;
        }

        return PayoutResult.builder()
                .totalWin(totalWin)
                .wins(wins)
                .triggerFreeSpin(isFreeSpinTriggered)
                .freeSpinCount(awardedFreeSpins)
                .scatterSymbol(config.scatterSymbolId())
                .scatterPositions(scatterWin != null ? scatterWin.getPositions() : new ArrayList<>())
                .triggerHoldAndWin(isHoldAndWinTriggered)
                .bonusInfos(bonusInfos)
                .build();
    }

    private List<PayoutResult.BonusInfo> scanBonusSymbols(Matrix matrix, SlotGameConfig config) {
        List<PayoutResult.BonusInfo> list = new ArrayList<>();
        for (int r = 0; r < matrix.rows(); r++) {
            for (int c = 0; c < matrix.cols(); c++) {
                int id = (int) matrix.getSymbolAt(r, c);
                if (id == SlotConstants.SYMBOL_CASH || id == SlotConstants.SYMBOL_MAJOR || id == SlotConstants.SYMBOL_MINI) {
                    
                    double mult = 0.0; 
                    String type = SlotConstants.TYPE_CASH;

                    if (id == SlotConstants.SYMBOL_MAJOR) {
                        mult = 100.0;
                        type = SlotConstants.TYPE_MAJOR;
                    } else if (id == SlotConstants.SYMBOL_MINI) {
                        mult = 25.0;
                        type = SlotConstants.TYPE_MINI;
                    }

                    list.add(PayoutResult.BonusInfo.builder()
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

    public double generateRandomCashMultiplier() {
        double[] opts = {1.0, 2.0, 3.0, 5.0, 10.0, 15.0, 20.0};
        return opts[random.nextInt(opts.length)];
    }


    private List<WinDetail> calculateAllPaylines(Matrix matrix, SlotGameConfig config, Money betPerLine) {
        List<WinDetail> lineWins = new ArrayList<>();
        for (int i = 0; i < config.paylines().size(); i++) {
            WinDetail win = calculateSingleLineWinDetail(matrix, config.paylines().get(i), i + 1, config, betPerLine);
            if (win != null) lineWins.add(win);
        }
        return lineWins;
    }

    private WinDetail calculateSingleLineWinDetail(Matrix matrix, Payline line, int lineId, SlotGameConfig config, Money betPerLine) {
        int baseId = -1; int match = 0;
        List<Integer> symbols = new ArrayList<>();
        for (int c = 0; c < matrix.cols(); c++) symbols.add((int) matrix.getSymbolAt(line.getRowAt(c), c));

        for (int c = 0; c < matrix.cols(); c++) {
            int id = symbols.get(c);
            if (id == config.scatterSymbolId() || id == SlotConstants.SYMBOL_CASH || 
                id == SlotConstants.SYMBOL_MAJOR || id == SlotConstants.SYMBOL_MINI) break;
            
            if (c == 0) { baseId = id; match = 1; continue; }
            if (id == baseId || id == config.wildSymbolId() || id == SlotConstants.DEFAULT_SYMBOL_WILD) match++;
            else if (baseId == config.wildSymbolId() || baseId == SlotConstants.DEFAULT_SYMBOL_WILD) { baseId = id; match++; }
            else break;
        }
        SlotSymbol sym = config.symbols().get(baseId);
        if (sym == null || sym.getMultiplier(match) <= 0) return null;

        List<int[]> pos = new ArrayList<>();
        for (int i = 0; i < match; i++) pos.add(new int[]{i, line.getRowAt(i)});

        return WinDetail.builder().lineId(lineId).symbolId(baseId).count(match)
                .amount(betPerLine.times(sym.getMultiplier(match))).positions(pos).type("line").build();
    }

    private WinDetail calculateScatterWinDetail(Matrix matrix, SlotGameConfig config, Money totalBet) {
        int count = 0; List<int[]> pos = new ArrayList<>();
        for (int c = 0; c < matrix.cols(); c++) {
            for (int r = 0; r < matrix.rows(); r++) {
                if ((int) matrix.getSymbolAt(r, c) == config.scatterSymbolId()) { count++; pos.add(new int[]{c, r}); }
            }
        }
        SlotSymbol sym = config.symbols().get(config.scatterSymbolId());
        if (sym == null || sym.getMultiplier(count) <= 0) return null;
        return WinDetail.builder().lineId(0).symbolId(config.scatterSymbolId()).count(count)
                .amount(totalBet.times(sym.getMultiplier(count))).positions(pos).type("scatter").build();
    }
}