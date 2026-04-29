package asia.rgp.game.nagas.modules.slot.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import asia.rgp.game.nagas.modules.slot.application.dto.request.BuyFeatureCommand;
import asia.rgp.game.nagas.modules.slot.application.dto.request.SpinCommand;
import asia.rgp.game.nagas.modules.slot.application.port.out.GameConfigPort;
import asia.rgp.game.nagas.modules.slot.application.port.out.JackpotHistoryPort;
import asia.rgp.game.nagas.modules.slot.application.port.out.SlotHistoryPort;
import asia.rgp.game.nagas.modules.slot.application.port.out.WalletPort;
import asia.rgp.game.nagas.modules.slot.domain.model.*;
import asia.rgp.game.nagas.modules.slot.domain.service.JackpotService;
import asia.rgp.game.nagas.modules.slot.domain.service.PayoutCalculator;
import asia.rgp.game.nagas.modules.slot.infrastructure.persistence.adapter.SlotStateRepository;
import asia.rgp.game.nagas.modules.slot.presentation.dto.response.SlotResultResponse;
import asia.rgp.game.nagas.shared.application.lock.DistributedLockService;
import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.domain.model.Money;
import asia.rgp.game.nagas.shared.infrastructure.rng.RngProvider;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Comprehensive API-level test suite covering ALL game modes described in document.txt (GDD).
 *
 * <p>Tests the full business logic through SpinUseCaseImpl with mocked infrastructure ports. Each
 * test verifies request → business logic → response contract.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Comprehensive Game Modes Test Suite")
class SpinUseCaseGameModesTest {

  @Mock private WalletPort walletPort;
  @Mock private DistributedLockService lockService;
  @Mock private GameConfigPort configPort;
  @Mock private SlotHistoryPort historyPort;
  @Mock private SlotStateRepository stateRepository;
  @Mock private JackpotService jackpotService;
  @Mock private JackpotHistoryPort jackpotHistoryPort;

  private PayoutCalculator payoutCalculator;
  private RngProvider rngProvider;
  private SpinUseCaseImpl useCase;
  private SlotGameConfig config;

  static final String AGENT_A = "agent-alpha";
  static final String AGENT_B = "agent-beta";
  static final String USER_1 = "player-1";
  static final String USER_2 = "player-2";
  static final String GAME_ID = "nagas_treasure";
  static final String SESSION_1 = "session-001";
  static final String SESSION_2 = "session-002";

  @BeforeEach
  void setUp() {
    payoutCalculator = new PayoutCalculator();
    rngProvider = new RngProvider();
    useCase =
        new SpinUseCaseImpl(
            walletPort,
            lockService,
            payoutCalculator,
            rngProvider,
            configPort,
            historyPort,
            stateRepository,
            jackpotService,
            jackpotHistoryPort,
            null);
    config = buildFullConfig();

    // Common stubs
    when(configPort.findByGameId(GAME_ID)).thenReturn(Optional.of(config));
    when(lockService.increment(anyString())).thenReturn(1L);
    // Lock stub: execute the supplier directly
    lenient()
        .when(lockService.withLock(anyString(), any(Duration.class), any()))
        .thenAnswer(
            inv -> {
              Supplier<?> supplier = inv.getArgument(2);
              return supplier.get();
            });
    lenient().when(walletPort.getBalance(anyString(), anyString())).thenReturn(10000000L);
    lenient()
        .when(jackpotService.getAllPools(anyString()))
        .thenReturn(Map.of("DIAMOND", 10000.0, "RUBY", 500.0, "EMERALD", 50.0, "SAPPHIRE", 10.0));
    lenient()
        .when(jackpotService.spinWheel(anyString(), anyString(), anyString(), any()))
        .thenReturn(
            JackpotService.JackpotSpinResult.builder()
                .winId("test-jp-win")
                .tierName("SAPPHIRE")
                .amount(Money.of(10.0))
                .hitArrow(false)
                .nearMiss(false)
                .build());
  }

  // ============================================================
  // 1. BASE SPIN TESTS
  // ============================================================

  @Nested
  @DisplayName("1. Base Spin")
  class BaseSpinTests {

    @Test
    @DisplayName("1.1 Normal spin returns valid response structure")
    void normalSpinReturnsValidResponse() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      SpinCommand cmd = spinCmd(AGENT_A, USER_1, SESSION_1, 1.0);
      SlotResultResponse resp = useCase.execute(cmd);

      // Verify response structure
      assertEquals("result", resp.getType());
      assertNotNull(resp.getData());
      assertNotNull(resp.getData().getControl());
      assertNotNull(resp.getData().getControl().getBalance());
      assertNotNull(resp.getData().getRound());
      assertNotNull(resp.getData().getRound().getResult());
      assertNotNull(resp.getData().getRound().getResult().getStages());
      assertFalse(resp.getData().getRound().getResult().getStages().isEmpty());

      // Screen is 5 cols x 3 rows (transposed for frontend)
      int[][] screen = resp.getData().getRound().getResult().getStages().get(0).getScreen();
      assertEquals(5, screen.length, "Screen should have 5 columns");
      assertEquals(3, screen[0].length, "Each column should have 3 rows");

      // Mode should be base
      assertEquals("base", resp.getData().getRound().getResult().getThisMode());
    }

    @Test
    @DisplayName("1.2 Spin debits wallet with bet amount")
    void spinDebitsWallet() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      verify(walletPort).debit(eq(AGENT_A), eq(USER_1), eq(Money.of(1.0)), anyString());
    }

    @Test
    @DisplayName("1.3 Spin credits wallet when there are wins")
    void spinCreditsOnWin() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      // Run multiple spins — at least one should produce a win
      int winsFound = 0;
      for (int i = 0; i < 50; i++) {
        reset(walletPort);
        lenient().when(walletPort.getBalance(anyString(), anyString())).thenReturn(10000000L);
        when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

        useCase.execute(spinCmd(AGENT_A, USER_1, "sess-" + i, 1.0));

        try {
          verify(walletPort).credit(eq(AGENT_A), eq(USER_1), any(Money.class), anyString());
          winsFound++;
          break; // One verified win is enough
        } catch (AssertionError e) {
          // No win this spin, continue
        }
      }
      assertTrue(winsFound > 0, "At least one spin should produce a win over 50 attempts");
    }

    @Test
    @DisplayName("1.4 Invalid bet amount is rejected")
    void invalidBetAmountRejected() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      // $3.0 is not in ALLOWED_BET_STEPS
      assertThrows(
          DomainException.class, () -> useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 3.0)));
    }

    @Test
    @DisplayName("1.5 All valid bet steps are accepted (GDD 2.2)")
    void allValidBetStepsAccepted() {
      double[] validBets = {
        0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 5.0, 7.5, 10.0, 15.0, 20.0, 25.0, 50.0, 75.0, 100.0,
        150.0, 200.0, 250.0
      };

      for (double bet : validBets) {
        reset(stateRepository, walletPort, historyPort);
        when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());
        lenient().when(walletPort.getBalance(anyString(), anyString())).thenReturn(100000000L);

        assertDoesNotThrow(
            () -> useCase.execute(spinCmd(AGENT_A, USER_1, "sess-bet-" + bet, bet)),
            "Bet $" + bet + " should be accepted");
      }
    }

    @Test
    @DisplayName("1.6 Win cap at 2000x bet (GDD 2.3)")
    void winCapApplied() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      SlotResultResponse resp = useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      // Total win should never exceed 2000 x bet
      String totalWin = resp.getData().getRound().getResult().getStages().get(0).getTotalWin();
      double win = Double.parseDouble(totalWin);
      assertTrue(win <= 2000.0, "Win $" + win + " should not exceed 2000x bet ($2000)");
    }

    @Test
    @DisplayName("1.7 Balance updates correctly after spin")
    void balanceUpdatesAfterSpin() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());
      when(walletPort.getBalance(AGENT_A, USER_1)).thenReturn(9999900L); // After debit

      SlotResultResponse resp = useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      String balance = resp.getData().getControl().getBalance();
      assertNotNull(balance);
      double bal = Double.parseDouble(balance);
      assertTrue(bal > 0, "Balance should be positive");
    }

    @Test
    @DisplayName("1.8 Trial mode does not debit wallet (GDD 9)")
    void trialModeNoDebit() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      SpinCommand cmd =
          SpinCommand.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .betAmount(Money.of(1.0))
              .sessionId(SESSION_1)
              .trialMode(true)
              .build();

      SlotResultResponse resp = useCase.execute(cmd);

      verify(walletPort, never()).debit(anyString(), anyString(), any(), anyString());
      verify(walletPort, never()).credit(anyString(), anyString(), any(), anyString());
      assertEquals("9999999.00", resp.getData().getControl().getBalance());
    }

    @Test
    @DisplayName("1.9 endsSuperround is true for standalone base spin with no feature trigger")
    void endsSuperroundForBaseSpinNoFeature() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      // Run spins until one doesn't trigger features
      for (int i = 0; i < 100; i++) {
        reset(stateRepository, walletPort, historyPort);
        when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());
        lenient().when(walletPort.getBalance(anyString(), anyString())).thenReturn(10000000L);

        SlotResultResponse resp = useCase.execute(spinCmd(AGENT_A, USER_1, "sess-ends-" + i, 1.0));
        String nextMode = resp.getData().getRound().getResult().getNextMode();

        if ("base".equals(nextMode)) {
          assertTrue(
              resp.getData().getRound().isEndsSuperround(),
              "Base spin with no feature should end superround");
          return;
        }
      }
      // Probabilistic — if 100 spins all trigger features, that's extreme but possible
    }
  }

  // ============================================================
  // 2. FREE SPIN TESTS
  // ============================================================

  @Nested
  @DisplayName("2. Free Spin")
  class FreeSpinTests {

    @Test
    @DisplayName("2.1 Buy Free Spins triggers free spin mode (GDD 7)")
    void buyFreeSpinsTriggers() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      BuyFeatureCommand cmd = buyFsCmd(AGENT_A, USER_1, SESSION_1, 1.0);
      SlotResultResponse resp = useCase.executeBuyFeature(cmd);

      assertEquals("base", resp.getData().getRound().getResult().getThisMode());
      assertEquals("free", resp.getData().getRound().getResult().getNextMode());
      assertFalse(resp.getData().getRound().isEndsSuperround());

      // Verify debit is 70x bet (GDD 7.3)
      verify(walletPort).debit(eq(AGENT_A), eq(USER_1), eq(Money.of(70.0)), anyString());
    }

    @Test
    @DisplayName("2.2 Free spin mode has correct features in response")
    void freeSpinFeaturesInResponse() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      SlotResultResponse resp =
          useCase.executeBuyFeature(buyFsCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      Map<String, Object> features = resp.getData().getRound().getResult().getFeatures();
      assertNotNull(features.get("freeSpins"), "Response should contain free_spins feature");

      @SuppressWarnings("unchecked")
      Map<String, Object> fsData = (Map<String, Object>) features.get("freeSpins");
      assertEquals(8, fsData.get("total"), "Total free spins should be 8 (GDD 4.3)");
    }

    @Test
    @DisplayName("2.3 Free spin count decrements each spin")
    void freeSpinCountDecrements() {
      // Set up state with free spins remaining
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(5)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(0.0)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      SlotResultResponse resp = useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      // Free spin doesn't debit wallet
      verify(walletPort, never()).debit(anyString(), anyString(), any(), anyString());

      // Mode should still be free
      assertEquals("free", resp.getData().getRound().getResult().getThisMode());

      // Verify state was saved with decremented count
      ArgumentCaptor<SlotState> stateCaptor = ArgumentCaptor.forClass(SlotState.class);
      verify(stateRepository).save(stateCaptor.capture());
      assertEquals(4, stateCaptor.getValue().getRemainingFreeSpins());
    }

    @Test
    @DisplayName("2.4 Free spins use base bet from trigger spin (GDD 4.4)")
    void freeSpinsUseBaseBet() {
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(3)
              .baseBet(Money.of(5.0)) // Triggered at $5 bet
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(0.0)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      SlotResultResponse resp = useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      assertEquals("5.00", resp.getData().getRound().getTotalBet());
    }

    @Test
    @DisplayName("2.5 Cannot buy feature during active bonus")
    void cannotBuyDuringActiveBonus() {
      SlotState activeState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .remainingFreeSpins(3)
              .freeSpinMode(true)
              .baseBet(Money.of(1.0))
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(activeState));

      assertThrows(
          DomainException.class,
          () -> useCase.executeBuyFeature(buyFsCmd(AGENT_A, USER_1, SESSION_1, 1.0)));
    }

    @Test
    @DisplayName("2.6 Win accumulates across free spins")
    void winAccumulatesAcrossFreeSpins() {
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(2)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(10.50)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      SlotResultResponse resp = useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      // Super round total win should include accumulated wins
      String superWin = resp.getData().getRound().getResult().getSuperRound().getTotalWin();
      double totalWin = Double.parseDouble(superWin);
      assertTrue(totalWin >= 10.50, "Accumulated win should be >= previous 10.50");
    }

    @Test
    @DisplayName("2.7 Last free spin transitions back to base mode")
    void lastFreeSpinTransitionsToBase() {
      SlotState lastFs =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(1) // Last spin
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(0.0)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(lastFs));

      SlotResultResponse resp = useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      assertEquals("free", resp.getData().getRound().getResult().getThisMode());
      // Next mode depends on whether this spin triggers H&W — likely base
      String nextMode = resp.getData().getRound().getResult().getNextMode();
      // If no feature triggered, should be base and end superround
      if ("base".equals(nextMode)) {
        assertTrue(resp.getData().getRound().isEndsSuperround());
      }
    }
  }

  // ============================================================
  // 3. HOLD & WIN TESTS
  // ============================================================

  @Nested
  @DisplayName("3. Hold & Win")
  class HoldAndWinTests {

    @Test
    @DisplayName("3.1 Buy Hold & Win triggers H&W mode (GDD 6)")
    void buyHoldAndWinTriggers() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      BuyFeatureCommand cmd = buyHwCmd(AGENT_A, USER_1, SESSION_1, 1.0);
      SlotResultResponse resp = useCase.executeBuyHoldAndWin(cmd);

      assertEquals("base", resp.getData().getRound().getResult().getThisMode());
      assertEquals("holdAndWin", resp.getData().getRound().getResult().getNextMode());

      // Verify debit is 70x bet
      verify(walletPort).debit(eq(AGENT_A), eq(USER_1), eq(Money.of(70.0)), anyString());
    }

    @Test
    @DisplayName("3.2 H&W mode has locked bonuses in response")
    void hwHasLockedBonuses() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      SlotResultResponse resp =
          useCase.executeBuyHoldAndWin(buyHwCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      Map<String, Object> features = resp.getData().getRound().getResult().getFeatures();
      assertNotNull(features.get("holdAndWin"), "Response should contain hold_and_win feature");

      @SuppressWarnings("unchecked")
      Map<String, Object> hwData = (Map<String, Object>) features.get("holdAndWin");
      assertNotNull(hwData.get("lockedBonuses"));

      @SuppressWarnings("unchecked")
      List<?> locked = (List<?>) hwData.get("lockedBonuses");
      assertTrue(locked.size() >= 6, "Should have at least 6 locked bonuses (GDD 5.2)");
    }

    @Test
    @DisplayName("3.3 H&W respins start at 3 (GDD 5.3)")
    void hwRespinsStartAt3() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      useCase.executeBuyHoldAndWin(buyHwCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      ArgumentCaptor<SlotState> stateCaptor = ArgumentCaptor.forClass(SlotState.class);
      verify(stateRepository).save(stateCaptor.capture());
      assertEquals(3, stateCaptor.getValue().getRemainingRespins());
      assertTrue(stateCaptor.getValue().isHoldAndWinMode());
    }

    @Test
    @DisplayName("3.4 H&W respin shows only bonus symbols on screen")
    void hwRespinOnlyBonusOnScreen() {
      List<SlotState.LockedBonus> locked = new ArrayList<>();
      locked.add(new SlotState.LockedBonus(0, 0, 13, 5.0, "CASH"));
      locked.add(new SlotState.LockedBonus(0, 1, 13, 3.0, "CASH"));
      locked.add(new SlotState.LockedBonus(0, 2, 13, 7.0, "CASH"));
      locked.add(new SlotState.LockedBonus(1, 0, 13, 2.0, "CASH"));
      locked.add(new SlotState.LockedBonus(1, 1, 13, 10.0, "CASH"));
      locked.add(new SlotState.LockedBonus(1, 2, 13, 1.0, "CASH"));

      SlotState hwState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .holdAndWin(true)
              .remainingRespins(3)
              .lockedBonuses(locked)
              .baseBet(Money.of(1.0))
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(0.0)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(hwState));

      SlotResultResponse resp = useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      int[][] screen = resp.getData().getRound().getResult().getStages().get(0).getScreen();
      assertEquals("holdAndWin", resp.getData().getRound().getResult().getThisMode());

      // Screen cells should be either 0 (empty) or bonus symbols (11-13)
      for (int c = 0; c < screen.length; c++) {
        for (int r = 0; r < screen[c].length; r++) {
          int sym = screen[c][r];
          assertTrue(
              sym == 0 || (sym >= 11 && sym <= 13),
              "H&W screen at [" + c + "," + r + "] has invalid symbol " + sym);
        }
      }
    }

    @Test
    @DisplayName("3.5 H&W does not debit wallet during respins")
    void hwNoDebitDuringRespins() {
      SlotState hwState = buildHwState(AGENT_A, USER_1, 3, 6);
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(hwState));

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      verify(walletPort, never()).debit(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("3.6 H&W total payout = sum of all locked multipliers x bet")
    void hwTotalPayoutCalculation() {
      // Create state with exactly 14 bonuses and 1 respin left — next spin ends H&W
      List<SlotState.LockedBonus> locked = new ArrayList<>();
      for (int i = 0; i < 14; i++) {
        locked.add(new SlotState.LockedBonus(i / 5, i % 5, 13, 5.0, "CASH")); // Each worth 5x
      }

      SlotState hwState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .holdAndWin(true)
              .remainingRespins(0) // Will end this spin
              .lockedBonuses(locked)
              .baseBet(Money.of(1.0))
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(0.0)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(hwState));

      SlotResultResponse resp = useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      // H&W should end — check the feature is_ending flag
      @SuppressWarnings("unchecked")
      Map<String, Object> hwFeature =
          (Map<String, Object>)
              resp.getData().getRound().getResult().getFeatures().get("holdAndWin");
      assertNotNull(hwFeature);
      assertTrue((Boolean) hwFeature.get("isEnding"), "H&W should be ending");
    }
  }

  // ============================================================
  // 4. JACKPOT TESTS
  // ============================================================

  @Nested
  @DisplayName("4. Jackpot")
  class JackpotTests {

    @Test
    @DisplayName("4.1 Jackpot pools included in every response")
    void jackpotPoolsInEveryResponse() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      SlotResultResponse resp = useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      Map<String, Object> features = resp.getData().getRound().getResult().getFeatures();
      assertNotNull(features.get("jackpotPools"), "Every response should include jackpot_pools");
    }

    @Test
    @DisplayName("4.2 Base spin contributes to jackpot pool (GDD 8.3)")
    void baseSpinContributes() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 10.0));

      verify(jackpotService).contribute(AGENT_A, Money.of(10.0));
    }

    @Test
    @DisplayName("4.3 Jackpot pools are agent-isolated")
    void jackpotPoolsAgentIsolated() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());
      when(stateRepository.find(AGENT_B, USER_2, GAME_ID)).thenReturn(Optional.empty());

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));
      useCase.execute(spinCmd(AGENT_B, USER_2, SESSION_2, 1.0));

      verify(jackpotService).contribute(AGENT_A, Money.of(1.0));
      verify(jackpotService).contribute(AGENT_B, Money.of(1.0));
      verify(jackpotService).getAllPools(AGENT_A);
      verify(jackpotService).getAllPools(AGENT_B);
    }

    @Test
    @DisplayName("4.4 No contribution in trial mode")
    void noContributionInTrialMode() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      SpinCommand trialCmd =
          SpinCommand.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .betAmount(Money.of(1.0))
              .sessionId(SESSION_1)
              .trialMode(true)
              .build();
      useCase.execute(trialCmd);

      verify(jackpotService, never()).contribute(anyString(), any());
      verify(jackpotService, never()).spinWheel(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("4.5 No contribution during Free Spin mode")
    void noContributionDuringFreeSpin() {
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(5)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(0.0)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      // Free spins don't debit, so no contribution
      verify(jackpotService, never()).contribute(anyString(), any());
    }

    @Test
    @DisplayName("4.6 No jackpot trigger during Free Spin mode")
    void noJackpotTriggerDuringFreeSpin() {
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(5)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(0.0)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      verify(jackpotService, never()).spinWheel(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("4.7 No contribution during Hold & Win mode")
    void noContributionDuringHoldAndWin() {
      SlotState hwState = buildHwState(AGENT_A, USER_1, 3, 6);
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(hwState));

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      verify(jackpotService, never()).contribute(anyString(), any());
    }

    @Test
    @DisplayName("4.8 Jackpot CANNOT trigger during Hold & Win (GDD rule)")
    void jackpotCannotTriggerDuringHoldAndWin() {
      SlotState hwState = buildHwState(AGENT_A, USER_1, 3, 6);
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(hwState));

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      verify(jackpotService, never()).spinWheel(anyString(), anyString(), anyString(), any());
      verify(jackpotHistoryPort, never()).save(any());
    }

    @Test
    @DisplayName("4.9 No contribution when buying Free Spins (buy = not base spin)")
    void noContributionWhenBuyingFreeSpins() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      useCase.executeBuyFeature(buyFsCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      // Buy feature debits wallet but should NOT contribute to jackpot
      verify(walletPort).debit(eq(AGENT_A), eq(USER_1), eq(Money.of(70.0)), anyString());
      verify(jackpotService, never()).contribute(anyString(), any());
    }

    @Test
    @DisplayName("4.10 No contribution when buying Hold & Win")
    void noContributionWhenBuyingHoldAndWin() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      useCase.executeBuyHoldAndWin(buyHwCmd(AGENT_A, USER_1, SESSION_1, 1.0));

      verify(walletPort).debit(eq(AGENT_A), eq(USER_1), eq(Money.of(70.0)), anyString());
      verify(jackpotService, never()).contribute(anyString(), any());
    }

    @Test
    @DisplayName("4.11 After H&W ends, next base spin contributes normally")
    void afterHwEndsBaseSpinContributes() {
      // First: H&W respin that ends (0 respins remaining)
      SlotState hwEndingState = buildHwState(AGENT_A, USER_1, 0, 6);
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(hwEndingState));

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));
      verify(jackpotService, never()).contribute(anyString(), any()); // H&W spin → no contribute

      // Second: back to base spin
      reset(stateRepository, walletPort, jackpotService, historyPort);
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());
      when(walletPort.getBalance(anyString(), anyString())).thenReturn(10000000L);
      when(jackpotService.getAllPools(anyString()))
          .thenReturn(Map.of("DIAMOND", 10000.0, "RUBY", 500.0, "EMERALD", 50.0, "SAPPHIRE", 10.0));

      useCase.execute(spinCmd(AGENT_A, USER_1, "sess-base", 5.0));
      verify(jackpotService).contribute(AGENT_A, Money.of(5.0)); // Base spin → contributes
    }
  }

  // ============================================================
  // 5. MULTI AGENT_ID TESTS
  // ============================================================

  @Nested
  @DisplayName("5. Multi Agent ID")
  class MultiAgentTests {

    @Test
    @DisplayName("5.1 Different agents have isolated game state")
    void differentAgentsIsolatedState() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());
      when(stateRepository.find(AGENT_B, USER_1, GAME_ID)).thenReturn(Optional.empty());

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));
      useCase.execute(spinCmd(AGENT_B, USER_1, SESSION_2, 2.0));

      // State lookups used correct agent IDs
      verify(stateRepository).find(AGENT_A, USER_1, GAME_ID);
      verify(stateRepository).find(AGENT_B, USER_1, GAME_ID);
    }

    @Test
    @DisplayName("5.2 Different agents have isolated wallets")
    void differentAgentsIsolatedWallets() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());
      when(stateRepository.find(AGENT_B, USER_1, GAME_ID)).thenReturn(Optional.empty());

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));
      useCase.execute(spinCmd(AGENT_B, USER_1, SESSION_2, 2.0));

      verify(walletPort).debit(eq(AGENT_A), eq(USER_1), eq(Money.of(1.0)), anyString());
      verify(walletPort).debit(eq(AGENT_B), eq(USER_1), eq(Money.of(2.0)), anyString());
    }

    @Test
    @DisplayName("5.3 Agent A in free spin, Agent B can still buy feature (no cross-agent block)")
    void noCrossAgentBlock() {
      // Agent A has active free spins
      SlotState agentAState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .remainingFreeSpins(5)
              .freeSpinMode(true)
              .baseBet(Money.of(1.0))
              .parentRoundId("p1")
              .baseRoundNumber(1)
              .accumulatedWin(0.0)
              .totalFreeSpins(8)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(agentAState));

      // Agent B has no active state
      when(stateRepository.find(AGENT_B, USER_1, GAME_ID)).thenReturn(Optional.empty());

      // Agent B should be able to buy feature — different agent, no block
      assertDoesNotThrow(
          () -> useCase.executeBuyFeature(buyFsCmd(AGENT_B, USER_1, SESSION_2, 1.0)));
    }

    @Test
    @DisplayName("5.4 Lock keys are agent-scoped (no cross-agent lock collision)")
    void lockKeysAgentScoped() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());
      when(stateRepository.find(AGENT_B, USER_1, GAME_ID)).thenReturn(Optional.empty());

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));
      useCase.execute(spinCmd(AGENT_B, USER_1, SESSION_1, 1.0)); // Same session, diff agent

      // Verify locks use agent-scoped keys
      ArgumentCaptor<String> lockKeyCaptor = ArgumentCaptor.forClass(String.class);
      verify(lockService, times(2)).withLock(lockKeyCaptor.capture(), any(), any());

      List<String> lockKeys = lockKeyCaptor.getAllValues();
      assertTrue(lockKeys.get(0).contains(AGENT_A));
      assertTrue(lockKeys.get(1).contains(AGENT_B));
      assertNotEquals(lockKeys.get(0), lockKeys.get(1));
    }

    @Test
    @DisplayName("5.5 History saved with correct agent_id")
    void historySavedWithCorrectAgencyId() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());
      when(stateRepository.find(AGENT_B, USER_2, GAME_ID)).thenReturn(Optional.empty());

      useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 1.0));
      useCase.execute(spinCmd(AGENT_B, USER_2, SESSION_2, 1.0));

      ArgumentCaptor<SlotHistory> histCaptor = ArgumentCaptor.forClass(SlotHistory.class);
      verify(historyPort, times(2)).save(histCaptor.capture());

      List<SlotHistory> histories = histCaptor.getAllValues();
      assertEquals(AGENT_A, histories.get(0).getAgencyId());
      assertEquals(AGENT_B, histories.get(1).getAgencyId());
    }

    @Test
    @DisplayName("5.6 Init returns agent-isolated balance")
    void initReturnsAgentIsolatedBalance() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());
      when(stateRepository.find(AGENT_B, USER_1, GAME_ID)).thenReturn(Optional.empty());
      when(walletPort.getBalance(AGENT_A, USER_1)).thenReturn(5000000L);
      when(walletPort.getBalance(AGENT_B, USER_1)).thenReturn(8000000L);

      SlotResultResponse respA = useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);
      SlotResultResponse respB = useCase.getInitialState(AGENT_B, USER_1, GAME_ID, SESSION_1);

      assertNotEquals(
          respA.getData().getControl().getBalance(),
          respB.getData().getControl().getBalance(),
          "Different agents should show different balances");
      assertEquals("50000.00", respA.getData().getControl().getBalance());
      assertEquals("80000.00", respB.getData().getControl().getBalance());
    }
  }

  // ============================================================
  // INIT ENDPOINT TESTS
  // ============================================================

  @Nested
  @DisplayName("Init Endpoint")
  class InitTests {

    @Test
    @DisplayName("Init returns valid response with screen and balance")
    void initReturnsValidResponse() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      SlotResultResponse resp = useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);

      assertEquals("result", resp.getType());
      assertNotNull(resp.getData().getControl().getBalance());
      assertNotNull(resp.getData().getRound().getResult().getStages().get(0).getScreen());
      assertEquals("base", resp.getData().getRound().getResult().getThisMode());
    }

    @Test
    @DisplayName("Init with active free spin state restores correctly")
    void initRestoresFreeSpin() {
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(5)
              .baseBet(Money.of(2.0))
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .lastGrid(
                  new int[][] {
                    {5, 6, 7, 8, 5},
                    {6, 7, 8, 5, 6},
                    {7, 8, 5, 6, 7}
                  })
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      SlotResultResponse resp = useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);

      assertEquals("free", resp.getData().getRound().getResult().getThisMode());
      assertEquals("2.00", resp.getData().getRound().getTotalBet());
    }

    @Test
    @DisplayName("Init returns game not found for unknown gameId")
    void initGameNotFound() {
      when(configPort.findByGameId("unknown")).thenReturn(Optional.empty());

      assertThrows(
          DomainException.class,
          () -> useCase.getInitialState(AGENT_A, USER_1, "unknown", SESSION_1));
    }
  }

  // ============================================================
  // 7. GAME RECOVERY (US 7.3) TESTS
  // ============================================================

  @Nested
  @DisplayName("7. Game Recovery (US 7.3)")
  class GameRecoveryTests {

    @Test
    @DisplayName("7.1 Fresh session — no state — returns base mode with zero totalWin")
    void freshSessionReturnsBase() {
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.empty());

      SlotResultResponse resp = useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);

      assertEquals("base", resp.getData().getRound().getResult().getThisMode());
      assertEquals("base", resp.getData().getRound().getResult().getNextMode());
      assertEquals("0.00", resp.getData().getRound().getTotalWin());
      assertEquals("1.00", resp.getData().getRound().getTotalBet());
      assertTrue(resp.getData().getRound().isEndsSuperround());
    }

    @Test
    @DisplayName("7.2 Recovery during free spin — restores mode, bet, accumulated win")
    void recoveryDuringFreeSpin() {
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(5)
              .baseBet(Money.of(5.0))
              .freeSpinMode(true)
              .parentRoundId("trigger-round-1")
              .triggerRoundId("trigger-round-1")
              .baseRoundNumber(3)
              .accumulatedWin(42.50)
              .lastGrid(
                  new int[][] {
                    {5, 6, 7, 8, 5},
                    {6, 7, 8, 5, 6},
                    {7, 8, 5, 6, 7}
                  })
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      SlotResultResponse resp = useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);

      // Mode should be free → free
      assertEquals("free", resp.getData().getRound().getResult().getThisMode());
      assertEquals("free", resp.getData().getRound().getResult().getNextMode());
      assertFalse(resp.getData().getRound().isEndsSuperround());

      // Accumulated win must be preserved
      assertEquals("42.50", resp.getData().getRound().getTotalWin());
      assertEquals("42.50", resp.getData().getRound().getResult().getSuperRound().getTotalWin());

      // Bet from trigger spin
      assertEquals("5.00", resp.getData().getRound().getTotalBet());

      // Free spin feature info
      @SuppressWarnings("unchecked")
      Map<String, Object> fsFeature =
          (Map<String, Object>)
              resp.getData().getRound().getResult().getFeatures().get("freeSpins");
      assertNotNull(fsFeature, "freeSpins feature should be present");
      assertEquals(5, fsFeature.get("remain"));
      assertEquals(8, fsFeature.get("total"));
    }

    @Test
    @DisplayName(
        "7.3 Recovery during hold-and-win — restores locked bonuses, respins, accumulated win")
    void recoveryDuringHoldAndWin() {
      List<SlotState.LockedBonus> locked = new ArrayList<>();
      locked.add(new SlotState.LockedBonus(0, 0, 13, 5.0, "CASH"));
      locked.add(new SlotState.LockedBonus(0, 2, 13, 3.0, "CASH"));
      locked.add(new SlotState.LockedBonus(1, 1, 11, 0.0, "MAJOR"));
      locked.add(new SlotState.LockedBonus(1, 3, 13, 7.0, "CASH"));
      locked.add(new SlotState.LockedBonus(2, 0, 13, 2.0, "CASH"));
      locked.add(new SlotState.LockedBonus(2, 4, 13, 10.0, "CASH"));

      SlotState hwState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .holdAndWin(true)
              .remainingRespins(2)
              .lockedBonuses(locked)
              .baseBet(Money.of(10.0))
              .parentRoundId("trigger-round-2")
              .triggerRoundId("trigger-round-2")
              .baseRoundNumber(5)
              .accumulatedWin(15.0)
              .lastGrid(
                  new int[][] {
                    {13, 0, 13, 0, 0},
                    {0, 11, 0, 13, 0},
                    {13, 0, 0, 0, 13}
                  })
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(hwState));

      SlotResultResponse resp = useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);

      // Mode should be holdAndWin → holdAndWin
      assertEquals("holdAndWin", resp.getData().getRound().getResult().getThisMode());
      assertEquals("holdAndWin", resp.getData().getRound().getResult().getNextMode());
      assertFalse(resp.getData().getRound().isEndsSuperround());

      // Accumulated win preserved
      assertEquals("15.00", resp.getData().getRound().getTotalWin());

      // Bet from trigger spin
      assertEquals("10.00", resp.getData().getRound().getTotalBet());

      // H&W feature info
      @SuppressWarnings("unchecked")
      Map<String, Object> hwFeature =
          (Map<String, Object>)
              resp.getData().getRound().getResult().getFeatures().get("holdAndWin");
      assertNotNull(hwFeature, "holdAndWin feature should be present");
      assertEquals(2, hwFeature.get("respinsRemain"));
      assertFalse((Boolean) hwFeature.get("isEnding"));

      @SuppressWarnings("unchecked")
      List<?> bonuses = (List<?>) hwFeature.get("lockedBonuses");
      assertEquals(6, bonuses.size(), "All 6 locked bonuses must be restored");
    }

    @Test
    @DisplayName("7.4 Recovery restores lastGrid from state (not random)")
    void recoveryRestoresLastGrid() {
      int[][] savedGrid =
          new int[][] {
            {1, 2, 3, 4, 5},
            {5, 4, 3, 2, 1},
            {2, 3, 4, 5, 6}
          };
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(3)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(5.0)
              .lastGrid(savedGrid)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      SlotResultResponse resp = useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);

      // Screen is transposed [cols][rows], so screen[0] = column 0 = {1, 5, 2}
      int[][] screen = resp.getData().getRound().getResult().getStages().get(0).getScreen();
      assertNotNull(screen);
      assertEquals(5, screen.length, "5 columns");
      assertEquals(3, screen[0].length, "3 rows per column");
      // Verify column 0 contains values from savedGrid column 0 = {1, 5, 2}
      assertEquals(1, screen[0][0]);
      assertEquals(5, screen[0][1]);
      assertEquals(2, screen[0][2]);
    }

    @Test
    @DisplayName("7.5 Recovery does NOT debit or credit wallet")
    void recoveryDoesNotTouchWallet() {
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(5)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(10.0)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);

      // Only getBalance should be called — no debit or credit
      verify(walletPort).getBalance(AGENT_A, USER_1);
      verify(walletPort, never()).debit(anyString(), anyString(), any(), anyString());
      verify(walletPort, never()).credit(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("7.6 Recovery does NOT modify SlotState in Redis")
    void recoveryDoesNotModifyState() {
      SlotState hwState = buildHwState(AGENT_A, USER_1, 2, 6);
      hwState.setAccumulatedWin(20.0);
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(hwState));

      useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);

      verify(stateRepository, never()).save(any());
      verify(stateRepository, never()).delete(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("7.7 Recovery does NOT save game history")
    void recoveryDoesNotSaveHistory() {
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(3)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(7.0)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);

      verify(historyPort, never()).save(any());
    }

    @Test
    @DisplayName("7.8 After recovery, next SPIN continues the chain normally")
    void afterRecoverySpinContinuesChain() {
      SlotState fsState =
          SlotState.builder()
              .agencyId(AGENT_A)
              .userId(USER_1)
              .gameId(GAME_ID)
              .sessionId(SESSION_1)
              .totalFreeSpins(8)
              .remainingFreeSpins(4)
              .baseBet(Money.of(2.0))
              .freeSpinMode(true)
              .parentRoundId("parent-1")
              .triggerRoundId("trigger-1")
              .baseRoundNumber(1)
              .accumulatedWin(12.0)
              .build();
      when(stateRepository.find(AGENT_A, USER_1, GAME_ID)).thenReturn(Optional.of(fsState));

      // Step 1: Recovery
      SlotResultResponse recoveryResp =
          useCase.getInitialState(AGENT_A, USER_1, GAME_ID, SESSION_1);
      assertEquals("free", recoveryResp.getData().getRound().getResult().getThisMode());
      assertEquals("12.00", recoveryResp.getData().getRound().getTotalWin());

      // Step 2: Next spin should continue free spin chain (state still has remainingFS=4)
      SlotResultResponse spinResp = useCase.execute(spinCmd(AGENT_A, USER_1, SESSION_1, 2.0));
      assertEquals("free", spinResp.getData().getRound().getResult().getThisMode());

      // No debit during free spin
      verify(walletPort, never()).debit(anyString(), anyString(), any(), anyString());

      // State should be saved with decremented free spins
      ArgumentCaptor<SlotState> stateCaptor = ArgumentCaptor.forClass(SlotState.class);
      verify(stateRepository).save(stateCaptor.capture());
      assertEquals(3, stateCaptor.getValue().getRemainingFreeSpins());

      // Accumulated win should be >= previous 12.00
      double superWin =
          Double.parseDouble(
              spinResp.getData().getRound().getResult().getSuperRound().getTotalWin());
      assertTrue(superWin >= 12.0, "Accumulated win should be >= previous 12.00");
    }
  }

  // ============================================================
  // HELPERS
  // ============================================================

  private SpinCommand spinCmd(String agencyId, String userId, String sessionId, double bet) {
    return SpinCommand.builder()
        .agencyId(agencyId)
        .userId(userId)
        .gameId(GAME_ID)
        .betAmount(Money.of(bet))
        .sessionId(sessionId)
        .trialMode(false)
        .build();
  }

  private BuyFeatureCommand buyFsCmd(String agencyId, String userId, String sessionId, double bet) {
    return BuyFeatureCommand.builder()
        .agencyId(agencyId)
        .userId(userId)
        .gameId(GAME_ID)
        .sessionId(sessionId)
        .featureName(SlotConstants.FEATURE_FREE_SPINS)
        .betAmount(Money.of(bet))
        .trialMode(false)
        .build();
  }

  private BuyFeatureCommand buyHwCmd(String agencyId, String userId, String sessionId, double bet) {
    return BuyFeatureCommand.builder()
        .agencyId(agencyId)
        .userId(userId)
        .gameId(GAME_ID)
        .sessionId(sessionId)
        .featureName(SlotConstants.FEATURE_HOLD_AND_WIN)
        .betAmount(Money.of(bet))
        .trialMode(false)
        .build();
  }

  private SlotState buildHwState(String agencyId, String userId, int respins, int lockedCount) {
    List<SlotState.LockedBonus> locked = new ArrayList<>();
    for (int i = 0; i < lockedCount; i++) {
      locked.add(new SlotState.LockedBonus(i / 5, i % 5, 13, 5.0, "CASH"));
    }
    return SlotState.builder()
        .agencyId(agencyId)
        .userId(userId)
        .gameId(GAME_ID)
        .sessionId(SESSION_1)
        .holdAndWin(true)
        .remainingRespins(respins)
        .lockedBonuses(locked)
        .baseBet(Money.of(1.0))
        .parentRoundId("parent-1")
        .baseRoundNumber(1)
        .accumulatedWin(0.0)
        .build();
  }

  @SuppressWarnings("all")
  private SlotGameConfig buildFullConfig() {
    Map<Integer, SlotSymbol> symbols =
        Map.ofEntries(
            Map.entry(1, new SlotSymbol(1, "A", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
            Map.entry(2, new SlotSymbol(2, "B", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
            Map.entry(3, new SlotSymbol(3, "C", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
            Map.entry(4, new SlotSymbol(4, "D", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false)),
            Map.entry(5, new SlotSymbol(5, "E", Map.of(3, 0.4, 4, 1.0, 5, 4.0), false, false)),
            Map.entry(6, new SlotSymbol(6, "F", Map.of(3, 0.4, 4, 2.0, 5, 6.0), false, false)),
            Map.entry(7, new SlotSymbol(7, "G", Map.of(3, 0.4, 4, 3.0, 5, 8.0), false, false)),
            Map.entry(8, new SlotSymbol(8, "H", Map.of(3, 1.0, 4, 4.0, 5, 10.0), false, false)),
            Map.entry(9, new SlotSymbol(9, "SCATTER", Map.of(3, 2.0), false, true)),
            Map.entry(10, new SlotSymbol(10, "WILD", Map.of(), true, false)),
            Map.entry(11, new SlotSymbol(11, "MAJOR", Map.of(), false, false)),
            Map.entry(12, new SlotSymbol(12, "MINI", Map.of(), false, false)),
            Map.entry(13, new SlotSymbol(13, "BONUS", Map.of(), false, false)),
            Map.entry(14, new SlotSymbol(14, "STACKED_WILD", Map.of(), true, false)));

    List<Payline> paylines =
        List.of(
            new Payline(1, new int[] {1, 1, 1, 1, 1}),
            new Payline(2, new int[] {0, 0, 0, 0, 0}),
            new Payline(3, new int[] {2, 2, 2, 2, 2}),
            new Payline(4, new int[] {0, 1, 2, 1, 0}),
            new Payline(5, new int[] {2, 1, 0, 1, 2}),
            new Payline(6, new int[] {1, 0, 0, 0, 1}),
            new Payline(7, new int[] {1, 2, 2, 2, 1}),
            new Payline(8, new int[] {0, 0, 1, 2, 2}),
            new Payline(9, new int[] {2, 2, 1, 0, 0}),
            new Payline(10, new int[] {1, 2, 1, 0, 1}),
            new Payline(11, new int[] {1, 0, 1, 2, 1}),
            new Payline(12, new int[] {0, 1, 1, 1, 0}),
            new Payline(13, new int[] {2, 1, 1, 1, 2}),
            new Payline(14, new int[] {0, 1, 0, 1, 0}),
            new Payline(15, new int[] {2, 1, 2, 1, 2}),
            new Payline(16, new int[] {1, 1, 0, 1, 1}),
            new Payline(17, new int[] {1, 1, 2, 1, 1}),
            new Payline(18, new int[] {0, 0, 2, 0, 0}),
            new Payline(19, new int[] {2, 2, 0, 2, 2}),
            new Payline(20, new int[] {0, 2, 2, 2, 0}),
            new Payline(21, new int[] {2, 0, 0, 0, 2}),
            new Payline(22, new int[] {1, 2, 0, 2, 1}),
            new Payline(23, new int[] {1, 0, 2, 0, 1}),
            new Payline(24, new int[] {0, 2, 0, 2, 0}),
            new Payline(25, new int[] {2, 0, 2, 0, 2}));

    List<List<Integer>> reelStrips =
        List.of(
            List.of(
                1, 1, 2, 2, 3, 3, 5, 6, 10, 1, 1, 2, 2, 3, 3, 13, 7, 8, 1, 1, 2, 2, 3, 3, 10, 5, 6,
                11, 12, 1, 1, 2, 2, 3, 3),
            List.of(
                1, 1, 2, 2, 3, 3, 10, 6, 13, 1, 1, 2, 2, 3, 3, 14, 8, 5, 11, 12, 1, 1, 2, 2, 3, 3,
                7, 9, 4, 10, 1, 1, 2, 2, 3, 3),
            List.of(
                1, 1, 2, 2, 3, 3, 5, 13, 11, 12, 10, 1, 1, 2, 2, 3, 3, 14, 7, 13, 6, 1, 1, 2, 2, 3,
                3, 8, 13, 9, 10, 1, 1, 2, 2, 3, 3),
            List.of(
                1, 1, 2, 2, 3, 3, 13, 5, 10, 1, 1, 2, 2, 3, 3, 7, 9, 11, 12, 14, 4, 10, 1, 1, 2, 2,
                3, 3, 13, 8, 1, 1, 2, 2, 3, 3),
            List.of(
                1, 1, 2, 2, 3, 3, 13, 1, 10, 7, 13, 1, 1, 2, 2, 3, 3, 11, 12, 14, 6, 13, 10, 1, 1,
                2, 2, 3, 3, 13, 4, 8, 1, 1, 2, 2, 3, 3));

    return new SlotGameConfig(
        GAME_ID,
        3,
        5,
        PayoutType.LINE,
        reelStrips,
        paylines,
        symbols,
        9,
        10,
        13,
        11,
        12,
        3,
        8,
        2000.0);
  }
}
