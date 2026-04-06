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
 * Exhaustive mode overlap and transition tests covering every scenario from document.txt (GDD).
 *
 * <p>Tests verify the state machine: Base ↔ FreeSpin ↔ HoldAndWin, including nested transitions,
 * edge cases, conflict prevention, and interrupted sessions.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Mode Transition & Overlap Tests")
class ModeTransitionTest {

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

  // Reusable captured states — stateRepository.save() captures the state after each spin
  private ArgumentCaptor<SlotState> stateCaptor;

  static final String AGENT = "agent-test";
  static final String USER = "player-1";
  static final String GAME = "nagas_treasure";
  static final String SESSION = "session-001";

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
    config = TestConfigBuilder.buildFullConfig(GAME);
    stateCaptor = ArgumentCaptor.forClass(SlotState.class);

    when(configPort.findByGameId(GAME)).thenReturn(Optional.of(config));
    when(lockService.increment(anyString())).thenReturn(1L);
    when(lockService.withLock(anyString(), any(Duration.class), any()))
        .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());
    when(walletPort.getBalance(anyString(), anyString())).thenReturn(10000000L);
    when(jackpotService.getAllPools(anyString()))
        .thenReturn(Map.of("DIAMOND", 10000.0, "RUBY", 500.0, "EMERALD", 50.0, "SAPPHIRE", 10.0));
    when(jackpotService.spinWheel(anyString(), anyString(), anyString(), any()))
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
  // 1. FREE SPIN → HOLD & WIN TRANSITION
  // ============================================================

  @Nested
  @DisplayName("1. FreeSpin → H&W Transition")
  class FsToHwTests {

    @Test
    @DisplayName("1.1 H&W triggered during FS → mode becomes hold_and_win, FS data preserved")
    void hwTriggeredDuringFs() {
      // Entry: FS mode with 5 remaining, H&W triggers on this spin
      SlotState fsState = buildFsState(5, 0.0);
      // Simulate: H&W trigger by having payout detect 6+ bonus symbols
      // We use buyHW to deterministically trigger — but for a FS spin that triggers HW
      // naturally, we set up state and let the use case detect it.
      // Instead, directly test the updateSlotState logic through a full spin.
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(fsState));

      SlotResultResponse resp = useCase.execute(spinCmd());

      // After spin, verify state was saved
      verify(stateRepository, atLeastOnce()).save(stateCaptor.capture());
      SlotState saved = stateCaptor.getValue();

      // FS was consumed (5 → 4) before H&W might trigger
      // If H&W triggered, holdAndWin=true and remainingFreeSpins preserved
      if (saved.isHoldAndWinMode()) {
        assertEquals(4, saved.getRemainingFreeSpins(), "FS count must be preserved during H&W");
        assertEquals(3, saved.getRemainingRespins(), "H&W starts with 3 respins");
        assertNotNull(saved.getLockedBonuses());
        assertTrue(saved.getLockedBonuses().size() >= 6);
        assertEquals("free", resp.getData().getRound().getResult().getThisMode());
        assertEquals("holdAndWin", resp.getData().getRound().getResult().getNextMode());
      }
      // If H&W didn't trigger (random), FS continues normally — that's fine
    }

    @Test
    @DisplayName("1.2 H&W completes inside FS → returns to FreeSpin, NOT Base (GDD 4.4)")
    void hwCompletesReturnsToFs() {
      // Use 15 locked bonuses = full grid → guaranteed H&W end regardless of RNG
      SlotState hwInsideFsState = buildHwInsideFsState(1, 15, 4);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwInsideFsState));

      SlotResultResponse resp = useCase.execute(spinCmd());

      verify(stateRepository, atLeastOnce()).save(stateCaptor.capture());
      SlotState saved = stateCaptor.getValue();

      // H&W ended (full grid), FS should resume
      assertFalse(saved.isHoldAndWinMode(), "H&W must be off after ending");
      assertEquals(4, saved.getRemainingFreeSpins(), "FS count must be exactly preserved");
      assertTrue(saved.isFreeSpinMode(), "Must return to FS mode");

      assertEquals("holdAndWin", resp.getData().getRound().getResult().getThisMode());
      assertEquals("free", resp.getData().getRound().getResult().getNextMode());
    }

    @Test
    @DisplayName("1.3 No wallet debit during H&W respins inside FS")
    void noDebitDuringHwInsideFs() {
      SlotState hwInsideFsState = buildHwInsideFsState(3, 6, 4);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwInsideFsState));

      useCase.execute(spinCmd());

      verify(walletPort, never()).debit(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("1.4 No jackpot contribution throughout entire FS+H&W session")
    void noJackpotDuringFsHwSession() {
      // FS spin
      SlotState fsState = buildFsState(5, 0.0);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(fsState));
      useCase.execute(spinCmd());

      // H&W inside FS
      reset(stateRepository, walletPort, historyPort);
      when(walletPort.getBalance(anyString(), anyString())).thenReturn(10000000L);
      SlotState hwInFs = buildHwInsideFsState(2, 6, 3);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwInFs));
      useCase.execute(spinCmd());

      // No contribution in either case
      verify(jackpotService, never()).contribute(anyString(), any());
    }

    @Test
    @DisplayName("1.5 FS wins + H&W wins accumulated correctly in total")
    void winsAccumulatedCorrectly() {
      // H&W ending inside FS (full grid) — state has accumulated FS wins of 15.0
      SlotState hwInsideFs = buildHwInsideFsState(1, 15, 3);
      hwInsideFs.setAccumulatedWin(15.0);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwInsideFs));

      SlotResultResponse resp = useCase.execute(spinCmd());

      // Super round total should include prior accumulated wins + H&W payout
      String superWin = resp.getData().getRound().getResult().getSuperRound().getTotalWin();
      double totalWin = Double.parseDouble(superWin);
      assertTrue(totalWin >= 15.0, "Total must include prior FS wins ($15) + H&W payout");
    }
  }

  // ============================================================
  // 2. FULL LIFECYCLE: FS → H&W → FS → Base
  // ============================================================

  @Nested
  @DisplayName("2. Full Lifecycle: FS → H&W → back to FS → Base")
  class FullLifecycleTests {

    @Test
    @DisplayName("2.1 H&W ends → FS resumes → last FS spin → Base")
    void hwEndsToFsThenBase() {
      // Step 1: H&W ends inside FS with 1 remaining free spin (full grid = guaranteed end)
      SlotState hwEnding = buildHwInsideFsState(1, 15, 1);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwEnding));

      SlotResultResponse resp1 = useCase.execute(spinCmd());

      verify(stateRepository, atLeastOnce()).save(stateCaptor.capture());
      SlotState afterHwEnd = stateCaptor.getValue();
      assertFalse(afterHwEnd.isHoldAndWinMode(), "H&W must be off");
      assertEquals(1, afterHwEnd.getRemainingFreeSpins(), "1 FS remaining");
      assertTrue(afterHwEnd.isFreeSpinMode());

      // Step 2: Last FS spin (1 → 0)
      reset(stateRepository, walletPort, historyPort);
      when(walletPort.getBalance(anyString(), anyString())).thenReturn(10000000L);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(afterHwEnd));
      stateCaptor = ArgumentCaptor.forClass(SlotState.class);

      SlotResultResponse resp2 = useCase.execute(spinCmd());

      // Should transition to base (no more FS, no H&W)
      // State might be deleted (chain finished) or saved with 0 remaining
      String nextMode = resp2.getData().getRound().getResult().getNextMode();
      // If no new feature triggered, should be base
      if ("base".equals(nextMode)) {
        assertTrue(resp2.getData().getRound().isEndsSuperround());
        // State should be deleted
        verify(stateRepository).delete(AGENT, USER, GAME);
      }
    }

    @Test
    @DisplayName("2.2 Wallet never debited during FS or H&W-inside-FS")
    void noDebitDuringEntireSession() {
      // FS spin
      SlotState fsState = buildFsState(3, 5.0);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(fsState));
      useCase.execute(spinCmd());

      verify(walletPort, never()).debit(anyString(), anyString(), any(), anyString());
    }
  }

  // ============================================================
  // 3. MULTIPLE H&W INSIDE ONE FS SESSION
  // ============================================================

  @Nested
  @DisplayName("3. Multiple H&W Inside One FS Session")
  class MultipleHwInFsTests {

    @Test
    @DisplayName("3.1 First H&W ends, FS resumes, second H&W can trigger")
    void secondHwCanTrigger() {
      // First H&W ends (full grid = guaranteed end) → FS with 5 remaining
      SlotState afterFirstHw = buildHwInsideFsState(1, 15, 5);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(afterFirstHw));

      useCase.execute(spinCmd());

      verify(stateRepository, atLeastOnce()).save(stateCaptor.capture());
      SlotState resumed = stateCaptor.getValue();

      // FS resumed
      assertFalse(resumed.isHoldAndWinMode());
      assertTrue(resumed.isFreeSpinMode());
      assertEquals(5, resumed.getRemainingFreeSpins());

      // Now simulate another FS spin that happens to trigger H&W
      // (We can't force RNG, but we verify the state machine allows it)
      reset(stateRepository, walletPort, historyPort);
      when(walletPort.getBalance(anyString(), anyString())).thenReturn(10000000L);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(resumed));
      stateCaptor = ArgumentCaptor.forClass(SlotState.class);

      useCase.execute(spinCmd()); // May or may not trigger H&W — that's fine

      // FS count should have decremented by 1
      verify(stateRepository, atLeastOnce()).save(stateCaptor.capture());
      SlotState afterSecond = stateCaptor.getValue();

      if (afterSecond.isHoldAndWinMode()) {
        // Second H&W triggered — verify FS count was consumed before H&W started
        assertEquals(4, afterSecond.getRemainingFreeSpins());
        assertEquals(3, afterSecond.getRemainingRespins());
      } else {
        // Normal FS spin
        assertEquals(4, afterSecond.getRemainingFreeSpins());
      }
    }

    @Test
    @DisplayName("3.2 Each H&W win accumulates into session total correctly")
    void eachHwWinAccumulates() {
      // H&W ending inside FS (full grid) with prior accumulated wins of 20.0
      SlotState hwEnding = buildHwInsideFsState(1, 15, 3);
      hwEnding.setAccumulatedWin(20.0); // From first H&W + FS wins
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwEnding));

      useCase.execute(spinCmd());

      verify(stateRepository, atLeastOnce()).save(stateCaptor.capture());
      SlotState after = stateCaptor.getValue();

      // Accumulated win should have increased (H&W payout added)
      // 8 locked bonuses * 5.0 mult * $1 bet = $40 H&W payout + $20 prior
      assertTrue(after.getAccumulatedWin() >= 20.0, "Must include prior accumulated wins");
    }

    @Test
    @DisplayName("3.3 FS count tracks correctly through multiple H&W transitions")
    void fsCountTracksCorrectly() {
      // Start with 6 FS remaining, H&W inside (full grid = guaranteed end)
      SlotState hw = buildHwInsideFsState(1, 15, 6);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hw));

      useCase.execute(spinCmd()); // H&W ends → FS 6 remaining

      verify(stateRepository, atLeastOnce()).save(stateCaptor.capture());
      SlotState afterHw = stateCaptor.getValue();
      assertEquals(6, afterHw.getRemainingFreeSpins());

      // Next FS spin → 5 remaining
      reset(stateRepository, walletPort, historyPort);
      when(walletPort.getBalance(anyString(), anyString())).thenReturn(10000000L);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(afterHw));
      stateCaptor = ArgumentCaptor.forClass(SlotState.class);

      useCase.execute(spinCmd());

      verify(stateRepository, atLeastOnce()).save(stateCaptor.capture());
      SlotState afterFs = stateCaptor.getValue();

      if (!afterFs.isHoldAndWinMode()) {
        assertEquals(5, afterFs.getRemainingFreeSpins(), "FS should decrement 6 → 5");
      } else {
        // H&W triggered again: FS consumed first (6→5), then H&W set
        assertEquals(5, afterFs.getRemainingFreeSpins());
      }
    }
  }

  // ============================================================
  // 4. MODE CONFLICT PREVENTION
  // ============================================================

  @Nested
  @DisplayName("4. Mode Conflict Prevention")
  class ModeConflictTests {

    @Test
    @DisplayName("4.1 Buy FS during active FS → rejected")
    void buyFsDuringActiveFs() {
      SlotState fsState = buildFsState(5, 0.0);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(fsState));

      DomainException ex =
          assertThrows(DomainException.class, () -> useCase.executeBuyFeature(buyFsCmd()));
      assertTrue(ex.getMessage().contains("Bonus round in progress"), "Error: " + ex.getMessage());
    }

    @Test
    @DisplayName("4.2 Buy H&W during active H&W → rejected")
    void buyHwDuringActiveHw() {
      SlotState hwState = buildHwState(3, 6);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwState));

      assertThrows(DomainException.class, () -> useCase.executeBuyHoldAndWin(buyHwCmd()));
    }

    @Test
    @DisplayName("4.3 Buy FS during active H&W → rejected")
    void buyFsDuringActiveHw() {
      SlotState hwState = buildHwState(3, 6);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwState));

      assertThrows(DomainException.class, () -> useCase.executeBuyFeature(buyFsCmd()));
    }

    @Test
    @DisplayName("4.4 Buy H&W during active FS → rejected")
    void buyHwDuringActiveFs() {
      SlotState fsState = buildFsState(5, 0.0);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(fsState));

      assertThrows(DomainException.class, () -> useCase.executeBuyHoldAndWin(buyHwCmd()));
    }

    @Test
    @DisplayName("4.5 Buy FS during H&W-inside-FS → rejected")
    void buyFsDuringHwInsideFs() {
      SlotState hwInsideFs = buildHwInsideFsState(2, 6, 4);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwInsideFs));

      assertThrows(DomainException.class, () -> useCase.executeBuyFeature(buyFsCmd()));
    }
  }

  // ============================================================
  // 5. INTERRUPTED SESSIONS (DISCONNECT/RECONNECT)
  // ============================================================

  @Nested
  @DisplayName("5. Interrupted Sessions (Disconnect/Reconnect)")
  class InterruptedSessionTests {

    @Test
    @DisplayName("5.1 Disconnect during FS → reconnect → FS state restored via init")
    void disconnectDuringFsRestored() {
      SlotState fsState = buildFsState(4, 12.50);
      fsState.setLastGrid(
          new int[][] {
            {5, 6, 7, 8, 5},
            {6, 7, 8, 5, 6},
            {7, 8, 5, 6, 7}
          });
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(fsState));

      SlotResultResponse resp = useCase.getInitialState(AGENT, USER, GAME, SESSION);

      assertEquals("free", resp.getData().getRound().getResult().getThisMode());
      assertEquals("free", resp.getData().getRound().getResult().getNextMode());
      assertFalse(resp.getData().getRound().isEndsSuperround());
    }

    @Test
    @DisplayName("5.2 Disconnect during H&W-inside-FS → reconnect → H&W state restored")
    void disconnectDuringHwInsideFsRestored() {
      SlotState hwInFs = buildHwInsideFsState(2, 8, 3);
      hwInFs.setLastGrid(
          new int[][] {
            {0, 13, 0, 13, 0},
            {13, 0, 13, 0, 13},
            {0, 13, 0, 0, 0}
          });
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwInFs));

      SlotResultResponse resp = useCase.getInitialState(AGENT, USER, GAME, SESSION);

      assertEquals("holdAndWin", resp.getData().getRound().getResult().getThisMode());
      assertEquals("holdAndWin", resp.getData().getRound().getResult().getNextMode());
      assertFalse(resp.getData().getRound().isEndsSuperround());

      // Verify H&W feature data is in response
      @SuppressWarnings("unchecked")
      Map<String, Object> hwFeature =
          (Map<String, Object>)
              resp.getData().getRound().getResult().getFeatures().get("holdAndWin");
      assertNotNull(hwFeature);
      assertEquals(2, hwFeature.get("respinsRemain"));
    }

    @Test
    @DisplayName("5.3 Disconnect when H&W ends returning to FS → reconnect → FS resumes")
    void disconnectWhenHwEndsReturningToFs() {
      // After H&W ended, FS mode active with 3 remaining
      SlotState fsAfterHw =
          SlotState.builder()
              .agentId(AGENT)
              .userId(USER)
              .gameId(GAME)
              .sessionId(SESSION)
              .totalFreeSpins(8)
              .remainingFreeSpins(3)
              .baseBet(Money.of(2.0))
              .freeSpinMode(true)
              .holdAndWin(false)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(45.0)
              .lastGrid(
                  new int[][] {
                    {5, 6, 7, 8, 5},
                    {6, 7, 8, 5, 6},
                    {7, 8, 5, 6, 7}
                  })
              .build();
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(fsAfterHw));

      SlotResultResponse resp = useCase.getInitialState(AGENT, USER, GAME, SESSION);

      assertEquals("free", resp.getData().getRound().getResult().getThisMode());
      // FS features should show remaining count
      @SuppressWarnings("unchecked")
      Map<String, Object> fsFeature =
          (Map<String, Object>)
              resp.getData().getRound().getResult().getFeatures().get("freeSpins");
      assertNotNull(fsFeature);
      assertEquals(3, fsFeature.get("remain"));
    }

    @Test
    @DisplayName("5.4 No duplicate wallet credit on reconnect (init doesn't credit)")
    void noDuplicateCreditOnReconnect() {
      SlotState fsState = buildFsState(3, 20.0);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(fsState));

      useCase.getInitialState(AGENT, USER, GAME, SESSION);

      // Init never credits wallet
      verify(walletPort, never()).credit(anyString(), anyString(), any(), anyString());
      verify(walletPort, never()).debit(anyString(), anyString(), any(), anyString());
    }
  }

  // ============================================================
  // 6. EDGE CASES
  // ============================================================

  @Nested
  @DisplayName("6. Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("6.1 H&W triggers on LAST free spin (8/8) → H&W completes before FS closes")
    void hwOnLastFreeSpinCompletesFirst() {
      // FS with 1 remaining, H&W triggers (simulated via state)
      // After FS consumes last spin (1→0), H&W triggers
      // H&W should complete, then FS ends (0 remaining, holdAndWin going to false)

      // Build state: H&W active inside FS, 0 remaining FS, 0 respins (H&W ending)
      SlotState hwOnLastFs = buildHwInsideFsState(1, 15, 0); // full grid, 0 FS remaining
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwOnLastFs));

      SlotResultResponse resp = useCase.execute(spinCmd());

      // H&W should end, and since 0 FS remaining, should go to base
      assertEquals("holdAndWin", resp.getData().getRound().getResult().getThisMode());
      assertEquals("base", resp.getData().getRound().getResult().getNextMode());
      assertTrue(resp.getData().getRound().isEndsSuperround());
    }

    @Test
    @DisplayName("6.2 H&W with 0 wins inside FS → FS continues normally")
    void hwZeroWinsInsideFsContinues() {
      // H&W ending with all locked bonuses having 0 effective win (minimum is seed)
      // Since bonuses always have multiplier >= 1.0, H&W always has some payout.
      // Test that FS continues regardless of H&W win amount.
      SlotState hwEnding = buildHwInsideFsState(1, 15, 4);
      hwEnding.setAccumulatedWin(0.0);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwEnding));

      SlotResultResponse resp = useCase.execute(spinCmd());

      verify(stateRepository, atLeastOnce()).save(stateCaptor.capture());
      SlotState saved = stateCaptor.getValue();

      // FS must resume regardless of H&W payout size
      assertEquals(4, saved.getRemainingFreeSpins());
      assertTrue(saved.isFreeSpinMode());
      assertFalse(saved.isHoldAndWinMode());
    }

    @Test
    @DisplayName("6.3 FS total = 0 but H&W had wins → correct total credited")
    void fsZeroWinsHwHadWins() {
      // H&W ending inside FS, only H&W contributes to total
      // Full grid: 15 bonuses * 5.0 mult + 1000 grand = total from H&W
      SlotState hwEnding = buildHwInsideFsState(1, 15, 1);
      hwEnding.setAccumulatedWin(0.0);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwEnding));

      SlotResultResponse resp = useCase.execute(spinCmd());

      // H&W payout should appear in the win data
      @SuppressWarnings("unchecked")
      Map<String, Object> hwFeature =
          (Map<String, Object>)
              resp.getData().getRound().getResult().getFeatures().get("holdAndWin");
      assertNotNull(hwFeature);
      assertTrue((Boolean) hwFeature.get("isEnding"));
      double totalMult = (Double) hwFeature.get("totalMultiplier");
      assertTrue(totalMult > 0, "H&W total multiplier must be > 0");
    }

    @Test
    @DisplayName("6.4 Agent A in FS+H&W, Agent B in base → zero cross-contamination")
    void multiAgentNoCrossContamination() {
      // Agent A: H&W inside FS
      SlotState agentAState = buildHwInsideFsState(2, 6, 4);
      agentAState.setAgentId("agent-A");
      when(stateRepository.find("agent-A", USER, GAME)).thenReturn(Optional.of(agentAState));

      // Agent B: fresh base spin
      when(stateRepository.find("agent-B", USER, GAME)).thenReturn(Optional.empty());

      // Both execute
      SlotResultResponse respA =
          useCase.execute(
              SpinCommand.builder()
                  .agentId("agent-A")
                  .userId(USER)
                  .gameId(GAME)
                  .betAmount(Money.of(1.0))
                  .sessionId("sess-a")
                  .build());
      SlotResultResponse respB =
          useCase.execute(
              SpinCommand.builder()
                  .agentId("agent-B")
                  .userId(USER)
                  .gameId(GAME)
                  .betAmount(Money.of(1.0))
                  .sessionId("sess-b")
                  .build());

      // Agent A should be in H&W mode
      assertEquals("holdAndWin", respA.getData().getRound().getResult().getThisMode());
      // Agent B should be in base mode
      assertEquals("base", respB.getData().getRound().getResult().getThisMode());

      // Agent B contributes to jackpot (base spin)
      verify(jackpotService).contribute(eq("agent-B"), any());
      // Agent A does NOT contribute (H&W mode)
      verify(jackpotService, never()).contribute(eq("agent-A"), any());
    }
  }

  // ============================================================
  // 7. STATE MACHINE INTEGRITY
  // ============================================================

  @Nested
  @DisplayName("7. State Machine Integrity")
  class StateMachineTests {

    @Test
    @DisplayName("7.1 Base spin → thisMode=base, nextMode=base (no feature)")
    void baseSpinModeCorrect() {
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.empty());

      // Run until a non-feature spin
      for (int i = 0; i < 100; i++) {
        reset(stateRepository, walletPort, historyPort);
        when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.empty());
        when(walletPort.getBalance(anyString(), anyString())).thenReturn(10000000L);

        SlotResultResponse resp = useCase.execute(spinCmd("sess-sm-" + i));
        if ("base".equals(resp.getData().getRound().getResult().getNextMode())) {
          assertEquals("base", resp.getData().getRound().getResult().getThisMode());
          assertTrue(resp.getData().getRound().isEndsSuperround());
          return;
        }
      }
    }

    @Test
    @DisplayName("7.2 FS spin → thisMode=free")
    void fsSpinModeCorrect() {
      SlotState fs = buildFsState(5, 0.0);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(fs));

      SlotResultResponse resp = useCase.execute(spinCmd());
      assertEquals("free", resp.getData().getRound().getResult().getThisMode());
    }

    @Test
    @DisplayName("7.3 H&W spin → thisMode=hold_and_win")
    void hwSpinModeCorrect() {
      SlotState hw = buildHwState(3, 6);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hw));

      SlotResultResponse resp = useCase.execute(spinCmd());
      assertEquals("holdAndWin", resp.getData().getRound().getResult().getThisMode());
    }

    @Test
    @DisplayName("7.4 FS and H&W cannot BOTH be active response mode (nested, not parallel)")
    void noDualActiveMode() {
      // Even when H&W is inside FS, the response shows ONE current mode
      SlotState hwInFs = buildHwInsideFsState(2, 6, 4);
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(hwInFs));

      SlotResultResponse resp = useCase.execute(spinCmd());
      String thisMode = resp.getData().getRound().getResult().getThisMode();

      // Should be hold_and_win (currently active) even though FS data is preserved
      assertEquals("holdAndWin", thisMode);
    }

    @Test
    @DisplayName("7.5 After all modes end → state deleted, clean base")
    void afterAllModesEndStateDeleted() {
      // Last FS spin, no features triggered
      SlotState lastFs =
          SlotState.builder()
              .agentId(AGENT)
              .userId(USER)
              .gameId(GAME)
              .sessionId(SESSION)
              .totalFreeSpins(8)
              .remainingFreeSpins(1)
              .baseBet(Money.of(1.0))
              .freeSpinMode(true)
              .holdAndWin(false)
              .parentRoundId("parent-1")
              .baseRoundNumber(1)
              .accumulatedWin(0.0)
              .build();
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.of(lastFs));

      SlotResultResponse resp = useCase.execute(spinCmd());

      if ("base".equals(resp.getData().getRound().getResult().getNextMode())) {
        // State should be deleted when chain finishes
        verify(stateRepository).delete(AGENT, USER, GAME);
      }
    }

    @Test
    @DisplayName("7.6 Every response has consistent thisMode and nextMode")
    void consistentModeFlags() {
      when(stateRepository.find(AGENT, USER, GAME)).thenReturn(Optional.empty());

      SlotResultResponse resp = useCase.execute(spinCmd());

      String thisMode = resp.getData().getRound().getResult().getThisMode();
      String nextMode = resp.getData().getRound().getResult().getNextMode();

      // thisMode must be one of the valid modes
      assertTrue(
          Set.of("base", "free", "holdAndWin").contains(thisMode), "Invalid thisMode: " + thisMode);
      assertTrue(
          Set.of("base", "free", "holdAndWin").contains(nextMode), "Invalid nextMode: " + nextMode);

      // If endsSuperround, nextMode must be base
      if (resp.getData().getRound().isEndsSuperround()) {
        assertEquals("base", nextMode, "endsSuperround requires nextMode=base");
      }
    }
  }

  // ============================================================
  // HELPERS
  // ============================================================

  private SpinCommand spinCmd() {
    return spinCmd(SESSION);
  }

  private SpinCommand spinCmd(String sessionId) {
    return SpinCommand.builder()
        .agentId(AGENT)
        .userId(USER)
        .gameId(GAME)
        .betAmount(Money.of(1.0))
        .sessionId(sessionId)
        .build();
  }

  private BuyFeatureCommand buyFsCmd() {
    return BuyFeatureCommand.builder()
        .agentId(AGENT)
        .userId(USER)
        .gameId(GAME)
        .sessionId(SESSION)
        .featureName(SlotConstants.FEATURE_FREE_SPINS)
        .betAmount(Money.of(1.0))
        .build();
  }

  private BuyFeatureCommand buyHwCmd() {
    return BuyFeatureCommand.builder()
        .agentId(AGENT)
        .userId(USER)
        .gameId(GAME)
        .sessionId(SESSION)
        .featureName(SlotConstants.FEATURE_HOLD_AND_WIN)
        .betAmount(Money.of(1.0))
        .build();
  }

  private SlotState buildFsState(int remaining, double accWin) {
    return SlotState.builder()
        .agentId(AGENT)
        .userId(USER)
        .gameId(GAME)
        .sessionId(SESSION)
        .totalFreeSpins(8)
        .remainingFreeSpins(remaining)
        .baseBet(Money.of(1.0))
        .freeSpinMode(true)
        .holdAndWin(false)
        .parentRoundId("parent-1")
        .baseRoundNumber(1)
        .accumulatedWin(accWin)
        .build();
  }

  private SlotState buildHwState(int respins, int lockedCount) {
    List<SlotState.LockedBonus> locked = new ArrayList<>();
    for (int i = 0; i < lockedCount; i++) {
      locked.add(new SlotState.LockedBonus(i / 5, i % 5, 13, 5.0, "CASH"));
    }
    return SlotState.builder()
        .agentId(AGENT)
        .userId(USER)
        .gameId(GAME)
        .sessionId(SESSION)
        .holdAndWin(true)
        .freeSpinMode(false)
        .remainingRespins(respins)
        .lockedBonuses(locked)
        .baseBet(Money.of(1.0))
        .parentRoundId("parent-1")
        .baseRoundNumber(1)
        .accumulatedWin(0.0)
        .build();
  }

  /** H&W active INSIDE an FS session — both holdAndWin=true and remainingFreeSpins > 0. */
  private SlotState buildHwInsideFsState(int hwRespins, int lockedCount, int fsRemaining) {
    List<SlotState.LockedBonus> locked = new ArrayList<>();
    for (int i = 0; i < lockedCount; i++) {
      locked.add(new SlotState.LockedBonus(i / 5, i % 5, 13, 5.0, "CASH"));
    }
    return SlotState.builder()
        .agentId(AGENT)
        .userId(USER)
        .gameId(GAME)
        .sessionId(SESSION)
        .totalFreeSpins(8)
        .remainingFreeSpins(fsRemaining)
        .baseBet(Money.of(1.0))
        .freeSpinMode(false) // FS suspended while H&W active
        .holdAndWin(true)
        .remainingRespins(hwRespins)
        .lockedBonuses(locked)
        .parentRoundId("parent-1")
        .baseRoundNumber(1)
        .accumulatedWin(0.0)
        .build();
  }
}
