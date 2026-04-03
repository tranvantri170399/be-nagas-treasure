package asia.rgp.game.nagas.modules.slot.application.usecase;

import asia.rgp.game.nagas.modules.slot.application.dto.request.BuyFeatureCommand;
import asia.rgp.game.nagas.modules.slot.application.dto.request.SpinCommand;
import asia.rgp.game.nagas.modules.slot.application.port.in.SpinUseCase;
import asia.rgp.game.nagas.modules.slot.application.port.out.*;
import asia.rgp.game.nagas.modules.slot.domain.model.*;
import asia.rgp.game.nagas.modules.slot.domain.service.JackpotService;
import asia.rgp.game.nagas.modules.slot.domain.service.PayoutCalculator;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.adapter.SlotStateRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
  private final JackpotService jackpotService;

  @Override
  public SlotResultResponse execute(SpinCommand command) {
    var stateOpt = stateRepository.find(command.getUserId(), command.getGameId());
    boolean isFS = stateOpt.isPresent() && stateOpt.get().isFreeSpinMode();
    boolean isHW = stateOpt.isPresent() && stateOpt.get().isHoldAndWinMode();

    Money displayBet = (isFS || isHW) ? stateOpt.get().getBaseBet() : command.getBetAmount();
    Money debitAmount = (isFS || isHW) ? Money.zero() : command.getBetAmount();

    return handleSpin(
        command.getGameId(),
        command.getUserId(),
        command.getSessionId(),
        displayBet,
        debitAmount,
        false,
        false,
        isFS,
        isHW,
        stateOpt.orElse(null));
  }

  private SlotResultResponse handleSpin(
      String gameId,
      String userId,
      String sessionId,
      Money displayBet,
      Money actualDebit,
      boolean isBuyFS,
      boolean isBuyHW,
      boolean wasFreeSpin,
      boolean wasHoldAndWin,
      SlotState currentState) {

    SlotGameConfig config =
        configPort
            .findByGameId(gameId)
            .orElseThrow(
                () -> new DomainException("Game config not found", ErrorCode.GAME_NOT_FOUND));
    String transactionId = UUID.randomUUID().toString();
    String lockKey = "spin:" + sessionId;

    final String parentTid =
        (currentState != null) ? currentState.getParentRoundId() : transactionId;
    // Ensure round counter is not null
    final int baseRound =
        (currentState != null)
            ? currentState.getBaseRoundNumber()
            : (int) lockService.increment("round_counter:" + sessionId);

    Map<String, Object> idWrapper = new HashMap<>();
    idWrapper.put("sessionId", sessionId);
    idWrapper.put("round", baseRound);
    idWrapper.put("id", transactionId);

    Supplier<SlotResultResponse> spinTask =
        () -> {
          if (actualDebit.isGreaterThanZero()) {
            walletPort.debit(userId, actualDebit, transactionId);
            jackpotService.contribute(actualDebit);
          }

          try {
            // --- STEP 1: GENERATE ORIGINAL GRID ---
            int[][] finalGrid;
            if (wasHoldAndWin) {
              finalGrid =
                  rngProvider.generateHoldAndWinGrid(config, currentState.getLockedBonuses());
            } else if (isBuyHW) {
              finalGrid = rngProvider.generateForcedBonusGrid(config, 6);
              expandStackedWilds(finalGrid, config);
            } else if (isBuyFS) {
              finalGrid = rngProvider.generateForcedScatterGrid(config, 3);
              expandStackedWilds(finalGrid, config);
            } else {
              finalGrid = rngProvider.generateGridFromStrips(config, wasFreeSpin);
              expandStackedWilds(finalGrid, config);
            }

            // --- STEP 2: EXPAND WILD (14 -> 10) BEFORE CREATING MATRIX ---
            // Only expand in Base and Free Games; Hold & Win does not expand Wilds
            Matrix matrix = new Matrix(config.rows(), config.cols(), finalGrid);

            // --- STEP 3: CALCULATE PAYOUT ---
            PayoutResult payoutResult =
                payoutCalculator.calculate(matrix, config, displayBet.divide(25), displayBet);

            Money jackpotWin = Money.zero();
            JackpotService.JackpotSpinResult jpResult = null;
            if (payoutResult.isJackpotTriggered() && !wasHoldAndWin) {
              jpResult = jackpotService.spinWheel(displayBet);
              jackpotWin = jpResult.getAmount();
            }

            final Money[] hwWinRef = {Money.zero()};
            SlotState updatedState =
                updateSlotState(
                    userId,
                    gameId,
                    sessionId,
                    transactionId,
                    baseRound,
                    parentTid,
                    displayBet,
                    config,
                    payoutResult,
                    wasFreeSpin,
                    wasHoldAndWin,
                    currentState,
                    isBuyFS,
                    isBuyHW,
                    hwWinRef);

            // --- STEP 4: AGGREGATE WINS ---
            Money normalWin = wasHoldAndWin ? hwWinRef[0] : payoutResult.getTotalWin();
            if (!wasHoldAndWin && hwWinRef[0].isGreaterThanZero()) normalWin = hwWinRef[0];

            Money maxWinLimit = displayBet.times(config.maxWinMultiplier());
            Money cappedNormalWin = normalWin.isGreaterThan(maxWinLimit) ? maxWinLimit : normalWin;
            Money finalTotalWin = cappedNormalWin.plus(jackpotWin);

            double currentCycleWin = finalTotalWin.getAmount();

            if (updatedState != null) {
              updatedState.setLastGrid(
                  prepareSnapshotGrid(
                      matrix.getGrid(),
                      updatedState.isHoldAndWinMode()
                          || (wasHoldAndWin && !updatedState.isHoldAndWinMode())));
              updatedState.setLastRoundId(transactionId);

              updatedState.addWin(currentCycleWin);
              currentCycleWin = updatedState.getAccumulatedWin();

              // Check if the game chain is completely finished
              boolean isChainFinished =
                  !updatedState.isHoldAndWinMode()
                      && !updatedState.isFreeSpinMode()
                      && updatedState.getRemainingFreeSpins() <= 0
                      && jpResult == null;

              if (isChainFinished) {
                stateRepository.delete(userId, gameId);
              } else {
                stateRepository.save(updatedState);
              }
            }

            if (finalTotalWin.isGreaterThanZero()) {
              walletPort.credit(userId, finalTotalWin, transactionId);
            }

            double balanceAfter = walletPort.getBalance(userId) / 100.0;
            saveGameHistory(
                userId,
                gameId,
                displayBet,
                actualDebit,
                matrix.getGrid(),
                finalTotalWin,
                idWrapper,
                (long) (balanceAfter * 100),
                updatedState,
                parentTid);

            return buildSlotResponse(
                config,
                matrix,
                payoutResult,
                displayBet,
                Money.of(currentCycleWin),
                hwWinRef[0],
                idWrapper,
                balanceAfter,
                25,
                (isBuyFS || isBuyHW),
                wasFreeSpin,
                wasHoldAndWin,
                updatedState,
                parentTid,
                jpResult);

          } catch (Exception e) {
            log.error("[Spin-Error] User {}: {}", userId, e.getMessage(), e);
            throw new DomainException("Spin failed", ErrorCode.INTERNAL_SERVER_ERROR);
          }
        };

    return lockService.withLock(lockKey, Duration.ofSeconds(5), spinTask);
  }

  /** EXPAND ONLY WHEN ID 14 IS ENCOUNTERED. Transforms the entire column to ID 10. */
  private void expandStackedWilds(int[][] grid, SlotGameConfig config) {
    for (int c = 0; c < config.cols(); c++) {
      boolean hasStacked = false;
      for (int r = 0; r < config.rows(); r++) {
        if (grid[r][c] == SlotConstants.SYMBOL_STACKED_WILD) { // ID 14
          hasStacked = true;
          break;
        }
      }
      if (hasStacked) {
        for (int r = 0; r < config.rows(); r++) {
          grid[r][c] = SlotConstants.DEFAULT_SYMBOL_WILD; // ID 10
        }
      }
    }
  }

  private SlotState updateSlotState(
      String userId,
      String gameId,
      String sessionId,
      String tid,
      int round,
      String parentTid,
      Money baseBet,
      SlotGameConfig config,
      PayoutResult payout,
      boolean wasFS,
      boolean wasHW,
      SlotState state,
      boolean isBuyFS,
      boolean isBuyHW,
      Money[] hwWinRef) {

    if (wasFS && state != null && !wasHW) {
      state.consumeFreeSpin();
      if (payout.isTriggerFreeSpin()) state.retrigger(payout.getFreeSpinCount());
    }

    if (!wasHW && (payout.isTriggerHoldAndWin() || isBuyHW)) {
      SlotState targetState =
          (state != null)
              ? state
              : SlotState.builder()
                  .userId(userId)
                  .gameId(gameId)
                  .sessionId(sessionId)
                  .baseBet(baseBet)
                  .parentRoundId(parentTid)
                  .baseRoundNumber(round)
                  .accumulatedWin(0.0)
                  .build();

      targetState.setHoldAndWin(true);
      targetState.setRemainingRespins(3);
      targetState.setLockedBonuses(
          new ArrayList<>(
              payout.getBonusInfos().stream()
                  .map(
                      b ->
                          new SlotState.LockedBonus(
                              b.getRow(),
                              b.getCol(),
                              b.getSymbolId(),
                              b.getMultiplier(),
                              b.getType()))
                  .collect(Collectors.toList())));
      return targetState;
    }

    if (wasHW && state != null) {
      Map<String, SlotState.LockedBonus> masterMap = new LinkedHashMap<>();
      state.getLockedBonuses().forEach(b -> masterMap.put(b.getRow() + ":" + b.getCol(), b));
      int countBefore = masterMap.size();

      for (var b : payout.getBonusInfos()) {
        String key = b.getRow() + ":" + b.getCol();
        if (!masterMap.containsKey(key)) {
          masterMap.put(
              key,
              new SlotState.LockedBonus(
                  b.getRow(), b.getCol(), b.getSymbolId(), b.getMultiplier(), b.getType()));
        }
      }
      state.setLockedBonuses(new ArrayList<>(masterMap.values()));

      if (masterMap.size() > countBefore) state.setRemainingRespins(3);
      else state.setRemainingRespins(state.getRemainingRespins() - 1);

      if (state.getLockedBonuses().size() >= 15 || state.getRemainingRespins() <= 0) {
        double totalMultiplier =
            state.getLockedBonuses().stream()
                .mapToDouble(SlotState.LockedBonus::getMultiplier)
                .sum();
        if (state.getLockedBonuses().size() >= 15) totalMultiplier += 1000.0;

        hwWinRef[0] = state.getBaseBet().times(totalMultiplier);
        state.setHoldAndWin(false);

        // Return to FREE SPIN mode if spins remain
        if (state.getRemainingFreeSpins() > 0) {
          state.setFreeSpinMode(true);
        }
      }
      return state;
    }

    if (!wasFS && (payout.isTriggerFreeSpin() || isBuyFS)) {
      int fsCount = isBuyFS ? 8 : payout.getFreeSpinCount();
      return SlotState.builder()
          .userId(userId)
          .gameId(gameId)
          .sessionId(sessionId)
          .totalFreeSpins(fsCount)
          .remainingFreeSpins(fsCount)
          .baseBet(baseBet)
          .accumulatedWin(0.0)
          .parentRoundId(parentTid)
          .baseRoundNumber(round)
          .freeSpinMode(true)
          .build();
    }

    return state;
  }

  private SlotResultResponse buildSlotResponse(
      SlotGameConfig config,
      Matrix matrix,
      PayoutResult payout,
      Money displayBet,
      Money totalAccumulatedWin,
      Money hwWinThisSpin,
      Map<String, Object> idWrapper,
      double balanceAfter,
      int lineCount,
      boolean isBuy,
      boolean wasFS,
      boolean wasHW,
      SlotState state,
      String parentTid,
      JackpotService.JackpotSpinResult jpResult) {

    boolean inHW = state != null && state.isHoldAndWinMode();
    boolean inFS = state != null && (state.getRemainingFreeSpins() > 0 || state.isFreeSpinMode());
    boolean isFSTriggerNow = payout.isTriggerFreeSpin() || (isBuy && !wasHW && !wasFS && inFS);
    boolean isEndingHWNow = wasHW && !inHW;

    Map<String, Object> features = new HashMap<>();
    features.put("jackpot_pools", jackpotService.getAllPools());

    if (inFS || isFSTriggerNow) {
      int remain =
          (state != null) ? state.getRemainingFreeSpins() : (isBuy ? 8 : payout.getFreeSpinCount());
      int total =
          (state != null) ? state.getTotalFreeSpins() : (isBuy ? 8 : payout.getFreeSpinCount());
      features.put(SlotConstants.FEATURE_FREE_SPINS, Map.of("remain", remain, "total", total));
    }

    if (inHW || wasHW) {
      List<SlotState.LockedBonus> bonuses =
          (state != null)
              ? state.getLockedBonuses()
              : payout.getBonusInfos().stream()
                  .map(
                      b ->
                          new SlotState.LockedBonus(
                              b.getRow(),
                              b.getCol(),
                              b.getSymbolId(),
                              b.getMultiplier(),
                              b.getType()))
                  .collect(Collectors.toList());
      double totalM = bonuses.stream().mapToDouble(SlotState.LockedBonus::getMultiplier).sum();
      if (bonuses.size() >= 15) totalM += 1000.0;
      features.put(
          SlotConstants.FEATURE_HOLD_AND_WIN,
          Map.of(
              "respins_remain",
              (state != null) ? Math.max(0, state.getRemainingRespins()) : 0,
              "locked_bonuses",
              bonuses,
              "total_multiplier",
              totalM,
              "is_ending",
              isEndingHWNow));
    }

    List<int[]> rings = payout.getGlowingRingPositions();
    if (!inHW && !wasHW && (!rings.isEmpty() || jpResult != null)) {
      Map<String, Object> jpData = new HashMap<>();
      jpData.put("glowing_rings", rings);
      if (jpResult != null) {
        jpData.put("tier", jpResult.getTierName());
        jpData.put("win", String.format("%.2f", jpResult.getAmount().getAmount()));
        jpData.put("hit_arrow", jpResult.isHitArrow());
        jpData.put("is_triggered", true);
      } else {
        jpData.put("is_triggered", false);
      }
      features.put("progressive_jackpot", jpData);
    }

    String totalWinChain = String.format("%.2f", totalAccumulatedWin.getAmount());
    boolean ends =
        (state == null
                || (!state.isHoldAndWinMode()
                    && !state.isFreeSpinMode()
                    && state.getRemainingFreeSpins() <= 0))
            && !isFSTriggerNow
            && jpResult == null;

    // --- CONVERT GRID TO screen[cols][rows] FOR FRONTEND ---
    int cols = matrix.cols();
    int rows = matrix.rows();
    int[][] feScreen = new int[cols][rows];

    for (int c = 0; c < cols; c++) {
      for (int r = 0; r < rows; r++) {
        int symbolId = matrix.getSymbolAt(r, c);
        if (inHW || wasHW) {
          feScreen[c][r] = (symbolId >= 11 && symbolId <= 13) ? symbolId : 0;
        } else {
          feScreen[c][r] = symbolId;
        }
      }
    }

    return SlotResultResponse.builder()
        .type("result")
        .data(
            SlotResultResponse.DataContent.builder()
                .control(
                    SlotResultResponse.ControlContent.builder()
                        .balance(String.format("%.2f", balanceAfter))
                        .build())
                .round(
                    SlotResultResponse.RoundContent.builder()
                        .transactionId(idWrapper)
                        .parentId(
                            Map.of(
                                "sessionId",
                                idWrapper.get("sessionId"),
                                "round",
                                idWrapper.get("round"),
                                "id",
                                parentTid))
                        .roundId(idWrapper.get("id").toString()) // RoundId from UUID
                        .totalWin(totalWinChain)
                        .totalBet(String.format("%.2f", displayBet.getAmount()))
                        .endsSuperround(ends)
                        .createdAt(Instant.now().toString())
                        .currency("USD")
                        .result(
                            SlotResultResponse.ResultDetails.builder()
                                .thisMode(wasHW ? "hold_and_win" : (wasFS ? "free" : "base"))
                                .nextMode(inHW ? "hold_and_win" : (inFS ? "free" : "base"))
                                .features(features)
                                .superRound(
                                    SlotResultResponse.SuperRound.builder()
                                        .totalWin(totalWinChain)
                                        .ends(ends)
                                        .build())
                                .stages(
                                    List.of(
                                        SlotResultResponse.StageContent.builder()
                                            .screen(feScreen)
                                            .totalWin(
                                                String.format(
                                                    "%.2f",
                                                    isEndingHWNow
                                                        ? hwWinThisSpin.getAmount()
                                                        : (wasHW
                                                            ? 0.0
                                                            : payout.getTotalWin().getAmount())))
                                            .wins(
                                                payout.getWins().stream()
                                                    .map(
                                                        w ->
                                                            SlotResultResponse.WinDetail.builder()
                                                                .type(w.getType())
                                                                .win(
                                                                    String.format(
                                                                        "%.2f",
                                                                        w.getAmount().getAmount()))
                                                                .payline(
                                                                    String.valueOf(w.getLineId()))
                                                                .symbol(w.getSymbolId())
                                                                .occurs(w.getCount())
                                                                .positions(
                                                                    w
                                                                        .getPositions()) // Positions are already [col, row] from PayoutCalculator
                                                                .build())
                                                    .collect(Collectors.toList()))
                                            .build()))
                                .build())
                        .build())
                .build())
        .build();
  }

  private int[][] prepareSnapshotGrid(int[][] grid, boolean isHW) {
    int rows = grid.length, cols = grid[0].length;
    int[][] clean = new int[rows][cols];
    for (int r = 0; r < rows; r++)
      for (int c = 0; c < cols; c++)
        clean[r][c] = (grid[r][c] >= 11 && grid[r][c] <= 13) ? grid[r][c] : (isHW ? 0 : grid[r][c]);
    return clean;
  }

  private void saveGameHistory(
      String userId,
      String gameId,
      Money dBet,
      Money actualDebit,
      int[][] grid,
      Money totalWin,
      Map<String, Object> idW,
      long bal,
      SlotState st,
      String pTid) {
    historyPort.save(
        SlotHistory.builder()
            .roundId(idW.get("id").toString())
            .parentRoundId(pTid)
            .userId(userId)
            .gameId(gameId)
            .totalBet((long) (actualDebit.getAmount() * 100))
            .totalWin((long) (totalWin.getAmount() * 100))
            .balanceAfter(bal)
            .screen(grid)
            .createdAt(Instant.now())
            .build());
  }

  @Override
  public SlotResultResponse executeBuyFeature(BuyFeatureCommand command) {
    checkActiveBonus(command.getUserId(), command.getGameId());
    return handleSpin(
        command.getGameId(),
        command.getUserId(),
        command.getSessionId(),
        command.getBetAmount(),
        command.calculateTotalCost(70.0),
        true,
        false,
        false,
        false,
        null);
  }

  @Override
  public SlotResultResponse executeBuyHoldAndWin(BuyFeatureCommand command) {
    checkActiveBonus(command.getUserId(), command.getGameId());
    return handleSpin(
        command.getGameId(),
        command.getUserId(),
        command.getSessionId(),
        command.getBetAmount(),
        command.calculateTotalCost(70.0),
        false,
        true,
        false,
        false,
        null);
  }

  @Override
  public SlotResultResponse getInitialState(String userId, String gameId, String sessionId) {
    SlotGameConfig config =
        configPort
            .findByGameId(gameId)
            .orElseThrow(
                () -> new DomainException("Game config not found", ErrorCode.GAME_NOT_FOUND));
    var stateOpt = stateRepository.find(userId, gameId);
    double balanceAfter = walletPort.getBalance(userId) / 100.0;
    SlotState state = stateOpt.orElse(null);
    Money displayBet = (state != null) ? state.getBaseBet() : Money.of(1.0);

    int[][] gridToUse =
        (state != null && state.getLastGrid() != null)
            ? state.getLastGrid()
            : rngProvider.generateGridFromStrips(config, false);
    // Ensure initial state also expands ID 14
    expandStackedWilds(gridToUse, config);

    Matrix matrix = new Matrix(config.rows(), config.cols(), gridToUse);

    return buildSlotResponse(
        config,
        matrix,
        PayoutResult.empty(),
        displayBet,
        Money.zero(),
        Money.zero(),
        Map.of(
            "id",
            "init-" + UUID.randomUUID().toString().substring(0, 8),
            "sessionId",
            sessionId,
            "round",
            (state != null ? state.getBaseRoundNumber() : 0)),
        balanceAfter,
        25,
        false,
        (state != null && state.isFreeSpinMode()),
        (state != null && state.isHoldAndWinMode()),
        state,
        (state != null ? state.getParentRoundId() : UUID.randomUUID().toString()),
        null);
  }

  private void checkActiveBonus(String userId, String gameId) {
    stateRepository
        .find(userId, gameId)
        .ifPresent(
            state -> {
              throw new DomainException("Bonus round in progress", ErrorCode.INTERNAL_SERVER_ERROR);
            });
  }
}
