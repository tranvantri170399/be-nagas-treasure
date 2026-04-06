package asia.rgp.game.nagas.modules.slot.application.usecase;

import asia.rgp.game.nagas.infrastructure.debug.CheatCode;
import asia.rgp.game.nagas.infrastructure.debug.CheatService;
import asia.rgp.game.nagas.modules.slot.application.dto.request.BuyFeatureCommand;
import asia.rgp.game.nagas.modules.slot.application.dto.request.SpinCommand;
import asia.rgp.game.nagas.modules.slot.application.port.in.SpinUseCase;
import asia.rgp.game.nagas.modules.slot.application.port.out.*;
import asia.rgp.game.nagas.modules.slot.domain.model.*;
import asia.rgp.game.nagas.modules.slot.domain.model.JackpotHistory;
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
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SpinUseCaseImpl implements SpinUseCase {

  private final WalletPort walletPort;
  private final DistributedLockService lockService;
  private final PayoutCalculator payoutCalculator;
  private final RngProvider rngProvider;
  private final GameConfigPort configPort;
  private final SlotHistoryPort historyPort;
  private final SlotStateRepository stateRepository;
  private final JackpotService jackpotService;
  private final JackpotHistoryPort jackpotHistoryPort;

  // Null in production — only injected when dev/staging profile active
  private final CheatService cheatService;

  public SpinUseCaseImpl(
      WalletPort walletPort,
      DistributedLockService lockService,
      PayoutCalculator payoutCalculator,
      RngProvider rngProvider,
      GameConfigPort configPort,
      SlotHistoryPort historyPort,
      SlotStateRepository stateRepository,
      JackpotService jackpotService,
      JackpotHistoryPort jackpotHistoryPort,
      @Autowired(required = false) CheatService cheatService) {
    this.walletPort = walletPort;
    this.lockService = lockService;
    this.payoutCalculator = payoutCalculator;
    this.rngProvider = rngProvider;
    this.configPort = configPort;
    this.historyPort = historyPort;
    this.stateRepository = stateRepository;
    this.jackpotService = jackpotService;
    this.jackpotHistoryPort = jackpotHistoryPort;
    this.cheatService = cheatService;
  }

  @Override
  public SlotResultResponse execute(SpinCommand command) {
    String agentId = command.getAgentId();
    var stateOpt = stateRepository.find(agentId, command.getUserId(), command.getGameId());
    boolean isFS = stateOpt.isPresent() && stateOpt.get().isFreeSpinMode();
    boolean isHW = stateOpt.isPresent() && stateOpt.get().isHoldAndWinMode();

    if (!isFS && !isHW) {
      validateBetAmount(command.getBetAmount());
    }

    Money displayBet = (isFS || isHW) ? stateOpt.get().getBaseBet() : command.getBetAmount();
    Money debitAmount = (isFS || isHW) ? Money.zero() : command.getBetAmount();

    return handleSpin(
        agentId,
        command.getGameId(),
        command.getUserId(),
        command.getSessionId(),
        displayBet,
        debitAmount,
        false,
        false,
        isFS,
        isHW,
        stateOpt.orElse(null),
        command.isTrialMode());
  }

  private SlotResultResponse handleSpin(
      String agentId,
      String gameId,
      String userId,
      String sessionId,
      Money displayBet,
      Money actualDebit,
      boolean isBuyFS,
      boolean isBuyHW,
      boolean wasFreeSpin,
      boolean wasHoldAndWin,
      SlotState currentState,
      boolean trialMode) {

    SlotGameConfig config =
        configPort
            .findByGameId(gameId)
            .orElseThrow(
                () -> new DomainException("Game config not found", ErrorCode.GAME_NOT_FOUND));
    String transactionId = UUID.randomUUID().toString();
    String lockKey = "spin:" + agentId + ":" + sessionId;

    // parentRoundId: null for BASE spin, triggerRoundId for FS/HW spins
    final String parentTid =
        (wasFreeSpin || wasHoldAndWin)
                && currentState != null
                && currentState.getTriggerRoundId() != null
            ? currentState.getTriggerRoundId()
            : null;
    final int baseRound =
        (currentState != null)
            ? currentState.getBaseRoundNumber()
            : (int) lockService.increment("round_counter:" + agentId + ":" + sessionId);

    Map<String, Object> idWrapper = new HashMap<>();
    idWrapper.put("sessionId", sessionId);
    idWrapper.put("round", baseRound);
    idWrapper.put("id", transactionId);

    Supplier<SlotResultResponse> spinTask =
        () -> {
          boolean isBaseSpin = !wasFreeSpin && !wasHoldAndWin && !isBuyFS && !isBuyHW;

          if (!trialMode && actualDebit.isGreaterThanZero()) {
            walletPort.debit(agentId, userId, actualDebit, transactionId);
            // GDD 8.3: Jackpot contribution only during base spin
            if (isBaseSpin) {
              jackpotService.contribute(agentId, actualDebit);
            }
          }

          try {
            // --- CHEAT: consume any active cheat for this user ---
            CheatService.ActiveCheat cheat =
                (cheatService != null) ? cheatService.consumeCheat(agentId, userId) : null;

            // --- STEP 1: GENERATE ORIGINAL GRID ---
            int[][] finalGrid;
            if (cheat != null && !wasHoldAndWin) {
              finalGrid = applyCheatGrid(cheat, config, wasFreeSpin);
            } else if (wasHoldAndWin) {
              finalGrid =
                  rngProvider.generateHoldAndWinGrid(config, currentState.getLockedBonuses());
            } else if (isBuyHW) {
              finalGrid = rngProvider.generateGridFromStrips(config, false);
              expandStackedWilds(finalGrid, config);
              forceBonusSymbols(finalGrid, config, 6);
            } else if (isBuyFS) {
              finalGrid = rngProvider.generateGridFromStrips(config, false);
              expandStackedWilds(finalGrid, config);
              forceScatterSymbols(finalGrid, config, 3);
            } else {
              finalGrid = rngProvider.generateGridFromStrips(config, wasFreeSpin);
              expandStackedWilds(finalGrid, config);
            }

            // --- STEP 2: CREATE MATRIX AND APPLY OVERLAYS ---
            Matrix matrix = new Matrix(config.rows(), config.cols(), finalGrid);

            // GDD 8.2: Glowing ring overlays only during base spin
            boolean forceJackpot =
                cheat != null
                    && (cheat.code() == CheatCode.FORCE_JACKPOT
                        || cheat.code() == CheatCode.FORCE_JACKPOT_TRIGGER);
            if (isBaseSpin) {
              rngProvider.applyJackpotOverlays(matrix, config, displayBet);
              if (forceJackpot) {
                rngProvider.forceJackpotOverlays(matrix, config, 6);
              }
            }

            // --- STEP 3: CALCULATE PAYOUT ---
            PayoutResult payoutResult = payoutCalculator.calculate(matrix, config, displayBet);

            Money jackpotWin = Money.zero();
            JackpotService.JackpotSpinResult jpResult = null;
            // GDD 8.2: Jackpot can only trigger during base spin (or cheat override)
            if ((payoutResult.isJackpotTriggered() || forceJackpot) && isBaseSpin) {
              jpResult = jackpotService.spinWheel(agentId, userId, sessionId, displayBet);
              jackpotWin = jpResult.getAmount();
              jackpotHistoryPort.save(
                  JackpotHistory.builder()
                      .winId(jpResult.getWinId())
                      .agentId(agentId)
                      .userId(userId)
                      .username(userId)
                      .sessionId(sessionId)
                      .jackpotType(jpResult.getTierName())
                      .amount(jpResult.getAmount().getAmount())
                      .createdAt(Instant.now())
                      .build());
            }

            final Money[] hwWinRef = {Money.zero()};
            SlotState updatedState =
                updateSlotState(
                    agentId,
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
                stateRepository.delete(agentId, userId, gameId);
              } else {
                stateRepository.save(updatedState);
              }
            }

            if (!trialMode && finalTotalWin.isGreaterThanZero()) {
              try {
                walletPort.credit(agentId, userId, finalTotalWin, transactionId);
                if (jpResult != null) {
                  jackpotService.markPaid(jpResult.getWinId());
                }
              } catch (Exception e) {
                if (jpResult != null) {
                  jackpotService.markFailed(jpResult.getWinId(), e.getMessage());
                }
                throw e;
              }
            } else if (jpResult != null) {
              jackpotService.markPaid(jpResult.getWinId());
            }

            double balanceAfter =
                trialMode
                    ? SlotConstants.TRIAL_MODE_BALANCE
                    : walletPort.getBalance(agentId, userId) / 100.0;
            saveGameHistory(
                agentId,
                userId,
                gameId,
                sessionId,
                displayBet,
                actualDebit,
                matrix.getGrid(),
                finalTotalWin,
                transactionId,
                parentTid,
                updatedState,
                wasFreeSpin,
                wasHoldAndWin,
                trialMode,
                payoutResult,
                jpResult,
                isBaseSpin);

            return buildSlotResponse(
                agentId,
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

  /**
   * Apply a cheat code to override grid generation. Returns a forced grid based on the cheat type.
   */
  @SuppressWarnings("unchecked")
  private int[][] applyCheatGrid(
      CheatService.ActiveCheat cheat, SlotGameConfig config, boolean wasFreeSpin) {
    int[][] grid;
    switch (cheat.code()) {
      case FORCE_FREE_SPIN -> {
        // Expand wilds FIRST, then place scatters on clean grid.
        grid = rngProvider.generateGridFromStrips(config, wasFreeSpin);
        expandStackedWilds(grid, config);
        forceScatterSymbols(grid, config, 3);
      }
      case FORCE_HOLD_AND_WIN, FORCE_HW_IN_FREE_SPIN -> {
        int count =
            cheat.value().containsKey("count")
                ? ((Number) cheat.value().get("count")).intValue()
                : 6;
        // Expand wilds FIRST, then force bonus symbols — prevents stacked wild
        // expansion from overwriting bonus symbols placed by generateForcedBonusGrid.
        grid = rngProvider.generateGridFromStrips(config, wasFreeSpin);
        expandStackedWilds(grid, config);
        forceBonusSymbols(grid, config, count);
      }
      case FORCE_HW_LOCKED_COUNT -> {
        int count = ((Number) cheat.value().getOrDefault("count", 6)).intValue();
        grid = rngProvider.generateGridFromStrips(config, wasFreeSpin);
        expandStackedWilds(grid, config);
        forceBonusSymbols(grid, config, count);
      }
      case FORCE_GRID -> {
        List<List<Integer>> rawGrid = (List<List<Integer>>) cheat.value().get("grid");
        if (rawGrid != null && rawGrid.size() == config.rows()) {
          grid = new int[config.rows()][config.cols()];
          for (int r = 0; r < config.rows(); r++) {
            for (int c = 0; c < config.cols(); c++) {
              grid[r][c] = rawGrid.get(r).get(c);
            }
          }
        } else {
          grid = rngProvider.generateGridFromStrips(config, wasFreeSpin);
          expandStackedWilds(grid, config);
        }
      }
      case FORCE_LOSS -> {
        // Adjacent columns share ZERO symbols → impossible to match 3+ on any payline.
        // Verified against all 25 paylines: zero wins.
        grid =
            new int[][] {
              {1, 4, 7, 2, 5},
              {2, 5, 8, 3, 6},
              {3, 6, 1, 4, 7}
            };
      }
      case FORCE_WIN_CAP -> {
        // All H symbols on every row — 25 paylines * 10.0 = 250x, but with high bet
        grid = new int[config.rows()][config.cols()];
        for (int r = 0; r < config.rows(); r++) {
          for (int c = 0; c < config.cols(); c++) {
            grid[r][c] = 8; // Symbol H (highest payout)
          }
        }
      }
      default -> {
        // For FORCE_NORMAL_WIN, FORCE_JACKPOT, etc. — use normal grid
        grid = rngProvider.generateGridFromStrips(config, wasFreeSpin);
        expandStackedWilds(grid, config);
      }
    }
    return grid;
  }

  /** Places exactly {@code count} scatters on columns 1-3 (reels 2/3/4), removing any existing. */
  private void forceScatterSymbols(int[][] grid, SlotGameConfig config, int count) {
    int scatterId = config.scatterSymbolId();
    // Clear existing scatters
    for (int r = 0; r < config.rows(); r++) {
      for (int c = 0; c < config.cols(); c++) {
        if (grid[r][c] == scatterId) grid[r][c] = 1;
      }
    }
    // Place scatters on columns 1, 2, 3 (reels 2/3/4 per GDD 4.2)
    int[] targetCols = {1, 2, 3};
    java.security.SecureRandom rng = new java.security.SecureRandom();
    for (int i = 0; i < count && i < targetCols.length; i++) {
      int row = rng.nextInt(config.rows());
      grid[row][targetCols[i]] = scatterId;
    }
  }

  /** Ensures the grid has at least {@code minCount} bonus symbols (id=13). */
  private void forceBonusSymbols(int[][] grid, SlotGameConfig config, int minCount) {
    int cashId = config.bonusSymbolId();
    int currentCount = 0;
    List<int[]> nonBonusCells = new ArrayList<>();

    for (int r = 0; r < config.rows(); r++) {
      for (int c = 0; c < config.cols(); c++) {
        int id = grid[r][c];
        if (id == cashId || id == SlotConstants.SYMBOL_MAJOR || id == SlotConstants.SYMBOL_MINI) {
          currentCount++;
        } else {
          nonBonusCells.add(new int[] {r, c});
        }
      }
    }

    if (currentCount < minCount) {
      java.util.Collections.shuffle(nonBonusCells);
      for (int i = 0; i < (minCount - currentCount) && i < nonBonusCells.size(); i++) {
        int[] pos = nonBonusCells.get(i);
        grid[pos[0]][pos[1]] = cashId;
      }
    }
  }

  private SlotState updateSlotState(
      String agentId,
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
                  .agentId(agentId)
                  .userId(userId)
                  .gameId(gameId)
                  .sessionId(sessionId)
                  .baseBet(baseBet)
                  .parentRoundId(parentTid)
                  .baseRoundNumber(round)
                  .accumulatedWin(0.0)
                  .build();

      // Set triggerRoundId: use existing FS trigger if nested, otherwise this spin
      if (targetState.getTriggerRoundId() == null) {
        targetState.setTriggerRoundId(tid);
      }
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
          .agentId(agentId)
          .userId(userId)
          .gameId(gameId)
          .sessionId(sessionId)
          .totalFreeSpins(fsCount)
          .remainingFreeSpins(fsCount)
          .baseBet(baseBet)
          .accumulatedWin(0.0)
          .parentRoundId(parentTid)
          .triggerRoundId(tid)
          .baseRoundNumber(round)
          .freeSpinMode(true)
          .build();
    }

    return state;
  }

  private SlotResultResponse buildSlotResponse(
      String agentId,
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
    features.put("jackpotPools", jackpotService.getAllPools(agentId));

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
              "respinsRemain",
              (state != null) ? Math.max(0, state.getRemainingRespins()) : 0,
              "lockedBonuses",
              bonuses,
              "totalMultiplier",
              totalM,
              "isEnding",
              isEndingHWNow));
    }

    List<int[]> rings = payout.getGlowingRingPositions();
    if (!inHW && !wasHW && (!rings.isEmpty() || jpResult != null)) {
      Map<String, Object> jpData = new HashMap<>();
      jpData.put("glowingRings", rings);
      if (jpResult != null) {
        jpData.put("tier", jpResult.getTierName());
        jpData.put("win", String.format("%.2f", jpResult.getAmount().getAmount()));
        jpData.put("hitArrow", jpResult.isHitArrow());
        jpData.put("isTriggered", true);
      } else {
        jpData.put("isTriggered", false);
      }
      features.put(SlotConstants.FEATURE_JACKPOT, jpData);
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
                        .type(
                            wasHW
                                ? SlotConstants.MODE_HOLD_AND_WIN
                                : (wasFS ? SlotConstants.MODE_FREE : SlotConstants.MODE_BASE))
                        .transactionId(idWrapper)
                        .parentId(
                            parentTid != null
                                ? Map.of(
                                    "sessionId",
                                    idWrapper.get("sessionId"),
                                    "round",
                                    idWrapper.get("round"),
                                    "id",
                                    parentTid)
                                : null)
                        .roundId(idWrapper.get("id").toString()) // RoundId from UUID
                        .parentRoundId(parentTid)
                        .thisMode(
                            wasHW
                                ? SlotConstants.MODE_HOLD_AND_WIN
                                : (wasFS ? SlotConstants.MODE_FREE : SlotConstants.MODE_BASE))
                        .nextMode(
                            inHW
                                ? SlotConstants.MODE_HOLD_AND_WIN
                                : (inFS ? SlotConstants.MODE_FREE : SlotConstants.MODE_BASE))
                        .totalWin(totalWinChain)
                        .totalBet(String.format("%.2f", displayBet.getAmount()))
                        .endsSuperround(ends)
                        .createdAt(Instant.now().toString())
                        .currency("USD")
                        .result(
                            SlotResultResponse.ResultDetails.builder()
                                .thisMode(
                                    wasHW
                                        ? SlotConstants.MODE_HOLD_AND_WIN
                                        : (wasFS
                                            ? SlotConstants.MODE_FREE
                                            : SlotConstants.MODE_BASE))
                                .nextMode(
                                    inHW
                                        ? SlotConstants.MODE_HOLD_AND_WIN
                                        : (inFS
                                            ? SlotConstants.MODE_FREE
                                            : SlotConstants.MODE_BASE))
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
      String agentId,
      String userId,
      String gameId,
      String sessionId,
      Money displayBet,
      Money actualDebit,
      int[][] grid,
      Money totalWin,
      String roundId,
      String parentRoundId,
      SlotState st,
      boolean wasFreeSpin,
      boolean wasHoldAndWin,
      boolean trialMode,
      PayoutResult payoutResult,
      JackpotService.JackpotSpinResult jpResult,
      boolean isBaseSpin) {

    // Determine thisMode
    String thisMode =
        wasHoldAndWin
            ? SlotConstants.MODE_HOLD_AND_WIN
            : (wasFreeSpin ? SlotConstants.MODE_FREE : SlotConstants.MODE_BASE);

    // Determine nextMode from updated state
    boolean inFS = st != null && st.isFreeSpinMode();
    boolean inHW = st != null && st.isHoldAndWinMode();
    String nextMode =
        inHW
            ? SlotConstants.MODE_HOLD_AND_WIN
            : (inFS ? SlotConstants.MODE_FREE : SlotConstants.MODE_BASE);

    // Bonus state snapshot
    Integer freeSpinsTotal =
        (st != null && (st.isFreeSpinMode() || st.getTotalFreeSpins() > 0))
            ? st.getTotalFreeSpins()
            : null;
    Integer freeSpinsRemain =
        (st != null && (st.isFreeSpinMode() || st.getRemainingFreeSpins() > 0))
            ? st.getRemainingFreeSpins()
            : null;
    Integer respinsRemain = (st != null && st.isHoldAndWinMode()) ? st.getRemainingRespins() : null;

    // Jackpot contribution (4% of bet during base spin per GDD 8.3)
    BigDecimal jackpotContribution =
        isBaseSpin
            ? BigDecimal.valueOf(actualDebit.getAmount() * 0.04)
                .setScale(2, java.math.RoundingMode.HALF_UP)
            : null;

    historyPort.save(
        SlotHistory.builder()
            .roundId(roundId)
            .parentRoundId(parentRoundId)
            .agentId(agentId)
            .userId(userId)
            .gameId(gameId)
            .sessionId(sessionId)
            .betAmount(BigDecimal.valueOf(displayBet.getAmount()))
            .totalWin(BigDecimal.valueOf(totalWin.getAmount()))
            .trialMode(trialMode)
            .screen(grid)
            .wins(payoutResult.getWins())
            .thisMode(thisMode)
            .nextMode(nextMode)
            .freeSpinsTotal(freeSpinsTotal)
            .freeSpinsRemain(freeSpinsRemain)
            .respinsRemain(respinsRemain)
            .jackpotWonTier(jpResult != null ? jpResult.getTierName() : null)
            .jackpotWonAmount(
                jpResult != null ? BigDecimal.valueOf(jpResult.getAmount().getAmount()) : null)
            .jackpotContribution(jackpotContribution)
            .timestamp(Instant.now())
            .build());
  }

  @Override
  public SlotResultResponse executeBuyFeature(BuyFeatureCommand command) {
    validateBetAmount(command.getBetAmount());
    checkActiveBonus(command.getAgentId(), command.getUserId(), command.getGameId());
    return handleSpin(
        command.getAgentId(),
        command.getGameId(),
        command.getUserId(),
        command.getSessionId(),
        command.getBetAmount(),
        command.calculateTotalCost(70.0),
        true,
        false,
        false,
        false,
        null,
        command.isTrialMode());
  }

  @Override
  public SlotResultResponse executeBuyHoldAndWin(BuyFeatureCommand command) {
    validateBetAmount(command.getBetAmount());
    checkActiveBonus(command.getAgentId(), command.getUserId(), command.getGameId());
    return handleSpin(
        command.getAgentId(),
        command.getGameId(),
        command.getUserId(),
        command.getSessionId(),
        command.getBetAmount(),
        command.calculateTotalCost(70.0),
        false,
        true,
        false,
        false,
        null,
        command.isTrialMode());
  }

  @Override
  public SlotResultResponse getInitialState(
      String agentId, String userId, String gameId, String sessionId) {
    SlotGameConfig config =
        configPort
            .findByGameId(gameId)
            .orElseThrow(
                () -> new DomainException("Game config not found", ErrorCode.GAME_NOT_FOUND));
    var stateOpt = stateRepository.find(agentId, userId, gameId);
    double balanceAfter = walletPort.getBalance(agentId, userId) / 100.0;
    SlotState state = stateOpt.orElse(null);
    Money displayBet = (state != null) ? state.getBaseBet() : Money.of(1.0);

    int[][] gridToUse =
        (state != null && state.getLastGrid() != null)
            ? state.getLastGrid()
            : rngProvider.generateGridFromStrips(config, false);
    expandStackedWilds(gridToUse, config);

    Matrix matrix = new Matrix(config.rows(), config.cols(), gridToUse);

    return buildSlotResponse(
        agentId,
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

  private void validateBetAmount(Money betAmount) {
    double amount = betAmount.getAmount();
    for (double step : SlotConstants.ALLOWED_BET_STEPS) {
      if (Math.abs(amount - step) < 0.001) return;
    }
    throw new DomainException("Invalid bet amount: " + amount, ErrorCode.INVALID_BET_AMOUNT);
  }

  private void checkActiveBonus(String agentId, String userId, String gameId) {
    stateRepository
        .find(agentId, userId, gameId)
        .ifPresent(
            state -> {
              throw new DomainException("Bonus round in progress", ErrorCode.FREE_GAME_ACTIVE);
            });
  }
}
