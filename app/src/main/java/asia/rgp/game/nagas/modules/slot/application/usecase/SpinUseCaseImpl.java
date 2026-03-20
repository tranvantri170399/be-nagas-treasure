package asia.rgp.game.nagas.modules.slot.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import asia.rgp.game.nagas.modules.slot.application.port.in.SpinUseCase;
import asia.rgp.game.nagas.modules.slot.application.port.out.GameConfigPort;
import asia.rgp.game.nagas.modules.slot.application.port.out.WalletPort;
import asia.rgp.game.nagas.modules.slot.application.port.out.SlotHistoryPort;
import asia.rgp.game.nagas.modules.slot.domain.model.*;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.adapter.SlotStateRepository;
import asia.rgp.game.nagas.modules.slot.domain.service.PayoutCalculator;
import asia.rgp.game.nagas.modules.slot.application.dto.request.SpinCommand;
import asia.rgp.game.nagas.modules.slot.application.dto.request.BuyFeatureCommand;
import asia.rgp.game.nagas.modules.slot.presentation.dto.response.SlotResultResponse;
import asia.rgp.game.nagas.shared.application.lock.DistributedLockService;
import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.domain.model.Matrix;
import asia.rgp.game.nagas.shared.domain.model.Money;
import asia.rgp.game.nagas.shared.error.ErrorCode;
import asia.rgp.game.nagas.shared.infrastructure.rng.RngProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpinUseCaseImpl implements SpinUseCase {

    private final WalletPort walletPort;
    private final DistributedLockService lockService;
    private final PayoutCalculator payoutCalculator;
    private final RngProvider rngProvider;
    private final GameConfigPort configPort;
    private final SlotHistoryPort historyPort;
    private final SlotStateRepository stateRepository;

    @Override
    public SlotResultResponse execute(SpinCommand command) {
        var stateOpt = stateRepository.find(command.getUserId(), command.getGameId());
        boolean isFreeSpin = stateOpt.isPresent() && stateOpt.get().isFreeSpinMode();
        boolean isHoldAndWin = stateOpt.isPresent() && stateOpt.get().isHoldAndWinMode();
        
        Money displayBet = (isFreeSpin || isHoldAndWin) ? stateOpt.get().getBaseBet() : command.getBetAmount();
        Money debitAmount = (isFreeSpin || isHoldAndWin) ? Money.zero() : command.getBetAmount();
        
        return handleSpin(command.getGameId(), command.getUserId(), command.getSessionId(), 
                         displayBet, debitAmount, false, false, isFreeSpin, isHoldAndWin, stateOpt.orElse(null));
    }

    @Override
    public SlotResultResponse executeBuyFeature(BuyFeatureCommand command) {
        Money buyPrice = command.calculateTotalCost(SlotConstants.BUY_FREE_SPINS_COST); 
        return handleSpin(command.getGameId(), command.getUserId(), command.getSessionId(), 
                         command.getBetAmount(), buyPrice, true, false, false, false, null);
    }

    @Override
    public SlotResultResponse executeBuyHoldAndWin(BuyFeatureCommand command) {
        Money buyPrice = command.calculateTotalCost(SlotConstants.BUY_HOLD_AND_WIN_COST); 
        return handleSpin(command.getGameId(), command.getUserId(), command.getSessionId(), 
                         command.getBetAmount(), buyPrice, false, true, false, false, null);
    }

    private SlotResultResponse handleSpin(String gameId, String userId, String sessionId, Money displayBet, Money actualDebit, 
                                        boolean isBuyFS, boolean isBuyHW, boolean isFreeSpin, boolean isHoldAndWin, SlotState currentState) {
        SlotGameConfig config = configPort.findByGameId(gameId)
                .orElseThrow(() -> new DomainException("Game config not found", ErrorCode.GAME_NOT_FOUND));

        String transactionId = UUID.randomUUID().toString();
        String lockKey = "spin:" + sessionId;
        
        final String parentTid = (currentState != null) ? currentState.getParentRoundId() : transactionId;
        final int baseRound = (currentState != null) ? currentState.getBaseRoundNumber() : (int) lockService.increment("round_counter:" + sessionId);

        Map<String, Object> idWrapper = Map.of("sessionId", sessionId, "round", baseRound, "id", transactionId);

        Supplier<SlotResultResponse> spinTask = () -> {
            if (actualDebit.isGreaterThanZero()) walletPort.debit(userId, actualDebit, transactionId);

            try {
                int[][] rawGrid = isBuyHW ? rngProvider.generateForcedBonusGrid(config, 6) 
                                : isBuyFS ? rngProvider.generateForcedScatterGrid(config, config.freeSpinTriggerCount())
                                : (isHoldAndWin && currentState != null) ? rngProvider.generateHoldAndWinGrid(config, currentState.getLockedBonuses())
                                : rngProvider.generateGridFromStrips(config);
                
                for (int c = 1; c < rawGrid.length; c++) {
                    boolean hasStacked = false;
                    for (int r = 0; r < rawGrid[c].length; r++) {
                        if (rawGrid[c][r] == SlotConstants.SYMBOL_STACKED_WILD) { hasStacked = true; break; }
                    }
                    if (hasStacked) {
                        for (int r = 0; r < rawGrid[c].length; r++) rawGrid[c][r] = SlotConstants.DEFAULT_SYMBOL_WILD;
                    }
                }

                Matrix matrix = new Matrix(config.rows(), config.cols(), rawGrid);
                int lineCount = (config.paylines() != null) ? config.paylines().size() : 25;
                Money betPerLine = displayBet.divide(lineCount);
                PayoutResult payoutResult = payoutCalculator.calculate(matrix, config, betPerLine, displayBet);
                
                if (payoutResult.getTotalWin().isGreaterThanZero()) walletPort.credit(userId, payoutResult.getTotalWin(), transactionId);

                final Money[] hwWinRef = {Money.zero()};
                SlotState updatedState = updateSlotState(userId, gameId, sessionId, transactionId, baseRound, parentTid, displayBet, config, payoutResult, isFreeSpin, isHoldAndWin, currentState, isBuyFS, isBuyHW, hwWinRef);

                // Lấy Balance thật: Wallet(Long) / 100 -> Tiền thật (Double)
                double balanceAfter = walletPort.getBalance(userId) / 100.0;

                saveGameHistory(userId, gameId, displayBet, actualDebit, rawGrid, payoutResult, idWrapper, (long)(balanceAfter * 100), isFreeSpin, isBuyFS, isBuyHW, updatedState, parentTid);

                return buildSlotResponse(config, matrix, payoutResult, displayBet, actualDebit, idWrapper, balanceAfter, lineCount, (isBuyFS || isBuyHW), isFreeSpin, isHoldAndWin, updatedState, parentTid, payoutResult.isTriggerHoldAndWin(), payoutResult.isTriggerFreeSpin(), hwWinRef[0]);

            } catch (Exception e) {
                log.error("[Spin-Error] User {}: {}", userId, e.getMessage(), e);
                throw new DomainException("Spin failed", ErrorCode.INTERNAL_SERVER_ERROR);
            }
        };
        return lockService.withLock(lockKey, Duration.ofSeconds(5), spinTask);
    }

    private SlotState updateSlotState(String userId, String gameId, String sessionId, String tid, int round, String parentTid, Money baseBet, 
                           SlotGameConfig config, PayoutResult payout, boolean isFreeSpin, boolean isHoldAndWin, SlotState state, boolean isBuyFS, boolean isBuyHW, Money[] hwWinRef) {
        
        // --- 1. TRIGGER HOLD AND WIN ---
        if (!isHoldAndWin && (payout.isTriggerHoldAndWin() || isBuyHW)) {
            List<SlotState.LockedBonus> initial = payout.getBonusInfos().stream()
                    .map(b -> {
                        double m = b.getMultiplier() > 0 ? b.getMultiplier() : payoutCalculator.generateRandomCashMultiplier();
                        return new SlotState.LockedBonus(b.getRow(), b.getCol(), b.getSymbolId(), m, b.getType());
                    }).collect(Collectors.toList());

            SlotState targetState = (isFreeSpin && state != null) ? state : SlotState.builder().userId(userId).gameId(gameId).sessionId(sessionId).baseBet(baseBet).parentRoundId(parentTid).baseRoundNumber(round).accumulatedWin(0.0).build();
            targetState.setHoldAndWin(true); 
            targetState.setRemainingRespins(3); 
            targetState.setLockedBonuses(initial); 
            targetState.addWin(payout.getTotalWin().getAmount());
            stateRepository.save(targetState); 
            return targetState;
        }

        // --- 2. RESPIN HOLD AND WIN ---
        if (isHoldAndWin && state != null) {
            Map<String, SlotState.LockedBonus> masterMap = new HashMap<>();
            state.getLockedBonuses().forEach(b -> masterMap.put(b.getRow() + ":" + b.getCol(), b));
            boolean hasNew = false;
            for (var b : payout.getBonusInfos()) {
                if (!masterMap.containsKey(b.getRow() + ":" + b.getCol())) {
                    double m = b.getMultiplier() > 0 ? b.getMultiplier() : payoutCalculator.generateRandomCashMultiplier();
                    masterMap.put(b.getRow() + ":" + b.getCol(), new SlotState.LockedBonus(b.getRow(), b.getCol(), b.getSymbolId(), m, b.getType()));
                    hasNew = true;
                }
            }
            state.setLockedBonuses(new ArrayList<>(masterMap.values()));
            state.setRemainingRespins(hasNew ? 3 : Math.max(0, state.getRemainingRespins() - 1));

            if (state.getLockedBonuses().size() >= 15 || state.getRemainingRespins() <= 0) {
                double totalM = state.getLockedBonuses().stream().mapToDouble(SlotState.LockedBonus::getMultiplier).sum();
                if (state.getLockedBonuses().size() >= 15) totalM += 1000.0;
                
                Money hwWin = state.getBaseBet().times(Math.min(totalM, 2000.0)); // Cap 2000x
                if (hwWin.isGreaterThanZero()) { 
                    walletPort.credit(userId, hwWin, tid); 
                    state.addWin(hwWin.getAmount()); 
                    hwWinRef[0] = hwWin; 
                }
                state.setHoldAndWin(false);
                if (state.getRemainingFreeSpins() > 0) stateRepository.save(state); else stateRepository.delete(userId, gameId);
            } else {
                stateRepository.save(state);
            }
            return state;
        }

        // --- 3. TRIGGER FREE SPIN ---
        if (!isFreeSpin && (payout.isTriggerFreeSpin() || isBuyFS)) {
            int fsCount = isBuyFS ? config.defaultFreeSpinCount() : payout.getFreeSpinCount();
            SlotState fsState = SlotState.builder().userId(userId).gameId(gameId).sessionId(sessionId).totalFreeSpins(fsCount).remainingFreeSpins(fsCount).baseBet(baseBet).accumulatedWin(payout.getTotalWin().getAmount()).parentRoundId(parentTid).baseRoundNumber(round).build();
            stateRepository.save(fsState); return fsState;
        }

        // --- 4.FREE SPIN ---
        if (isFreeSpin && state != null) {
            if (payout.isTriggerFreeSpin()) state.retrigger(payout.getFreeSpinCount());
            state.consumeFreeSpin(); 
            state.addWin(payout.getTotalWin().getAmount());
            if (state.getRemainingFreeSpins() > 0) stateRepository.save(state); else stateRepository.delete(userId, gameId);
            return state;
        }
        return null;
    }

    private SlotResultResponse buildSlotResponse(SlotGameConfig config, Matrix matrix, PayoutResult payout, Money displayBet, Money actualDebit, Map<String, Object> idWrapper, double balanceAfter, int lineCount, boolean isBuy, boolean isFreeSpin, boolean isHoldAndWin, SlotState state, String parentTid, boolean isTrigHW, boolean isTrigFS, Money hwWin) {
        Money totalWin = payout.getTotalWin().plus(hwWin);
        String tid = idWrapper.get("id").toString();
        
        int[][] screen = new int[matrix.cols()][matrix.rows()];
        for (int c = 0; c < matrix.cols(); c++) for (int r = 0; r < matrix.rows(); r++) screen[c][r] = (int) matrix.getSymbolAt(r, c);
        
        // Sticky Locked Bonuses for Hold & Win
        if (state != null && state.getLockedBonuses() != null) {
            for (var b : state.getLockedBonuses()) {
                if (b.getCol() < matrix.cols() && b.getRow() < matrix.rows()) screen[b.getCol()][b.getRow()] = b.getSymbolId();
            }
        }

        Map<String, Object> features = new HashMap<>();
        String thisMode = isHoldAndWin ? "hold_and_win" : (isFreeSpin ? "free" : "base");
        String nextMode = "base"; 
        boolean ends = true;

        if (isTrigHW || isHoldAndWin || (state != null && !state.isHoldAndWinMode() && state.getLockedBonuses() != null && !state.getLockedBonuses().isEmpty())) {
            boolean isEnding = (state != null && !state.isHoldAndWinMode());
            double totalM = (state != null) ? state.getLockedBonuses().stream().mapToDouble(SlotState.LockedBonus::getMultiplier).sum() : 0;
            if (state != null && state.getLockedBonuses().size() >= 15) totalM += 1000.0;

            features.put("hold_and_win", Map.of(
                "respins_remain", state != null ? state.getRemainingRespins() : 3, 
                "locked_bonuses", state != null ? state.getLockedBonuses() : payout.getBonusInfos(), 
                "total_multiplier", totalM, 
                "is_ending", isEnding
            ));
            if (!isEnding) { nextMode = "hold_and_win"; ends = false; } 
            else if (state != null && state.getRemainingFreeSpins() > 0) { nextMode = "free"; ends = false; }
        } else if (isTrigFS || isFreeSpin) {
            int rem = state != null ? state.getRemainingFreeSpins() : payout.getFreeSpinCount();
            features.put("free_spins", Map.of("remain", rem, "total", state != null ? state.getTotalFreeSpins() : payout.getFreeSpinCount()));
            if (rem > 0) { nextMode = "free"; ends = false; }
        }

        double accWin = state != null ? state.getAccumulatedWin() : totalWin.getAmount();

        return SlotResultResponse.builder()
            .type("result")
            .data(SlotResultResponse.DataContent.builder()
                .round(SlotResultResponse.RoundContent.builder()
                    .transactionId(idWrapper)
                    .parentId(Map.of("sessionId", idWrapper.get("sessionId"), "round", state != null ? state.getBaseRoundNumber() : idWrapper.get("round"), "id", parentTid))
                    .totalBet(formatMoney(actualDebit.isGreaterThanZero() ? actualDebit : displayBet))
                    .totalWin(formatMoney(totalWin))
                    .currency("USD")
                    .endsSuperround(ends)
                    .type(thisMode.toUpperCase())
                    .roundId(tid)
                    .createdAt(Instant.now().toString())
                    .result(SlotResultResponse.ResultDetails.builder()
                        .thisMode(thisMode)
                        .nextMode(nextMode)
                        .features(features)
                        .superround(SlotResultResponse.SuperRound.builder().totalWin(String.format("%.2f", accWin)).ends(ends).buyFeature(isBuy).build())
                        .stages(List.of(SlotResultResponse.StageContent.builder()
                            .wins(payout.getWins().stream().map(w -> SlotResultResponse.WinDetail.builder()
                                .type(w.getType()).win(formatMoney(w.getAmount())).payline(String.valueOf(w.getLineId())).symbol(w.getSymbolId()).positions(w.getPositions()).build()).collect(Collectors.toList()))
                            .screen(screen)
                            .totalWin(formatMoney(totalWin))
                            .build()))
                        .build()).build())
                .control(SlotResultResponse.ControlContent.builder().balance(String.format("%.2f", balanceAfter)).build())
                .build()).build();
    }

    private void saveGameHistory(String userId, String gameId, Money dBet, Money aDeb, int[][] grid, PayoutResult pay, Map<String, Object> idW, long bal, boolean isF, boolean isBF, boolean isBH, SlotState st, String pTid) {
        historyPort.save(SlotHistory.builder()
            .roundId(idW.get("id").toString())
            .parentRoundId(pTid)
            .userId(userId)
            .gameId(gameId)
            .totalBet((long)(aDeb.getAmount() * 100))
            .totalWin((long)(pay.getTotalWin().getAmount() * 100))
            .balanceAfter(bal)
            .screen(grid)
            .createdAt(Instant.now())
            .build());
    }

    private String formatMoney(Money m) { return String.format("%.2f", m.getAmount()); }
}