package asia.rgp.game.nagas.modules.slot.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import asia.rgp.game.nagas.shared.domain.model.Matrix;
import asia.rgp.game.nagas.shared.domain.model.Money;
import asia.rgp.game.nagas.shared.infrastructure.rng.RngProvider;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpinUseCaseRefundTest {

  @Mock private WalletPort walletPort;
  @Mock private DistributedLockService lockService;
  @Mock private GameConfigPort configPort;
  @Mock private SlotHistoryPort historyPort;
  @Mock private SlotStateRepository stateRepository;
  @Mock private JackpotService jackpotService;
  @Mock private JackpotHistoryPort jackpotHistoryPort;

  @Test
  void refundsWalletIfSpinFailsAfterDebit() {
    // Arrange
    SlotGameConfig config = buildMinimalConfig();

    // Force payout calculation to throw after wallet debit.
    PayoutCalculator throwingPayoutCalculator =
        new PayoutCalculator() {
          @Override
          public PayoutResult calculate(Matrix matrix, SlotGameConfig config, Money totalBet) {
            throw new RuntimeException("boom");
          }
        };

    RngProvider rngProvider = new RngProvider();
    SpinUseCaseImpl useCase =
        new SpinUseCaseImpl(
            walletPort,
            lockService,
            throwingPayoutCalculator,
            rngProvider,
            configPort,
            historyPort,
            stateRepository,
            jackpotService,
            jackpotHistoryPort,
            null);

    when(configPort.findByGameId(eq(config.gameId()))).thenReturn(Optional.of(config));
    when(stateRepository.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
    when(lockService.increment(anyString())).thenReturn(1L);
    // Execute lock supplier directly for deterministic testing.
    lenient()
        .when(lockService.withLock(anyString(), any(Duration.class), any()))
        .thenAnswer(
            inv -> {
              java.util.function.Supplier<SlotResultResponse> supplier = inv.getArgument(2);
              return supplier.get();
            });

    SpinCommand cmd =
        SpinCommand.builder()
            .agencyId("agent-1")
            .userId("user-1")
            .gameId(config.gameId())
            .sessionId("sess-1")
            .betAmount(Money.of(1.0))
            .trialMode(false)
            .build();

    // Act / Assert
    assertThrows(DomainException.class, () -> useCase.execute(cmd));

    // Wallet should be debited, then refunded.
    verify(walletPort).debit(eq("agent-1"), eq("user-1"), eq(Money.of(1.0)), anyString());
    verify(walletPort)
        .credit(
            eq("agent-1"), eq("user-1"), eq(Money.of(1.0)), argThat(tx -> tx.endsWith("-refund")));
  }

  private SlotGameConfig buildMinimalConfig() {
    // Minimal config for grid generation & validation; payout logic is forced to throw.
    int rows = 3;
    int cols = 5;

    Map<Integer, SlotSymbol> symbols =
        Map.of(
            SlotConstants.SYMBOL_A,
            new SlotSymbol(
                SlotConstants.SYMBOL_A, "A", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false),
            SlotConstants.SYMBOL_B,
            new SlotSymbol(
                SlotConstants.SYMBOL_B, "B", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false),
            SlotConstants.SYMBOL_C,
            new SlotSymbol(
                SlotConstants.SYMBOL_C, "C", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false),
            SlotConstants.SYMBOL_D,
            new SlotSymbol(
                SlotConstants.SYMBOL_D, "D", Map.of(3, 0.2, 4, 0.4, 5, 2.0), false, false),
            SlotConstants.DEFAULT_SYMBOL_SCATTER,
            new SlotSymbol(
                SlotConstants.DEFAULT_SYMBOL_SCATTER, "SCATTER", Map.of(3, 2.0), false, true),
            SlotConstants.DEFAULT_SYMBOL_WILD,
            new SlotSymbol(SlotConstants.DEFAULT_SYMBOL_WILD, "WILD", Map.of(), true, false),
            SlotConstants.SYMBOL_MAJOR,
            new SlotSymbol(SlotConstants.SYMBOL_MAJOR, "MAJOR", Map.of(), false, false),
            SlotConstants.SYMBOL_MINI,
            new SlotSymbol(SlotConstants.SYMBOL_MINI, "MINI", Map.of(), false, false),
            SlotConstants.SYMBOL_CASH,
            new SlotSymbol(SlotConstants.SYMBOL_CASH, "CASH", Map.of(), false, false));

    List<List<Integer>> reelStrips =
        List.of(
            List.of(1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 9),
            List.of(1, 2, 3, 4, 10, 11, 12, 13, 14, 9),
            List.of(1, 2, 3, 4, 10, 11, 12, 13, 14, 9),
            List.of(1, 2, 3, 4, 10, 11, 12, 13, 14, 9),
            List.of(1, 2, 3, 4, 10, 11, 12, 13, 14, 9));

    // One simple payline to satisfy "line-based games must have at least one payline".
    List<Payline> paylines = List.of(new Payline(1, new int[] {1, 1, 1, 1, 1}));

    return new SlotGameConfig(
        "nagas_treasure",
        rows,
        cols,
        PayoutType.LINE,
        reelStrips,
        paylines,
        symbols,
        SlotConstants.DEFAULT_SYMBOL_SCATTER /* scatterSymbolId */,
        SlotConstants.DEFAULT_SYMBOL_WILD /* wildSymbolId */,
        SlotConstants.SYMBOL_CASH /* bonusSymbolId */,
        SlotConstants.SYMBOL_MAJOR /* majorSymbolId */,
        SlotConstants.SYMBOL_MINI /* miniSymbolId */,
        3 /* freeSpinTriggerCount */,
        8 /* defaultFreeSpinCount */,
        2000.0 /* maxWinMultiplier */);
  }
}
