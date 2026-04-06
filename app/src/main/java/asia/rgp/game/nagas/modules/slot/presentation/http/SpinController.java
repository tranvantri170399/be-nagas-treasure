package asia.rgp.game.nagas.modules.slot.presentation.http;

import asia.rgp.game.nagas.modules.slot.application.dto.request.BuyFeatureCommand;
import asia.rgp.game.nagas.modules.slot.application.dto.request.SpinCommand;
import asia.rgp.game.nagas.modules.slot.application.port.in.SpinUseCase;
import asia.rgp.game.nagas.modules.slot.application.port.out.JackpotHistoryPort;
import asia.rgp.game.nagas.modules.slot.domain.model.JackpotHistory;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.modules.slot.domain.service.JackpotService;
import asia.rgp.game.nagas.modules.slot.presentation.dto.request.SpinRequest;
import asia.rgp.game.nagas.modules.slot.presentation.dto.response.SlotResultResponse;
import asia.rgp.game.nagas.shared.domain.model.Money;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/slot")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SpinController {

  private final SpinUseCase spinUseCase;
  private final JackpotService jackpotService;
  private final JackpotHistoryPort jackpotHistoryPort;

  @GetMapping("/init")
  public ResponseEntity<SlotResultResponse> initGame(
      @RequestParam String agentId,
      @RequestParam String userId,
      @RequestParam String gameId,
      @RequestParam String sessionId) {

    log.info(
        "[API-IN] Init Game: agent={}, user={}, game={}, sid={}",
        agentId,
        userId,
        gameId,
        sessionId);

    SlotResultResponse response = spinUseCase.getInitialState(agentId, userId, gameId, sessionId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/jackpot-pools")
  public ResponseEntity<?> getJackpotPools(@RequestParam String agentId) {
    return ResponseEntity.ok(Map.of("data", jackpotService.getAllPools(agentId)));
  }

  @PostMapping("/spin")
  public ResponseEntity<SlotResultResponse> spin(@Valid @RequestBody SpinRequest request) {
    log.info(
        "[API-IN] Spin: agent={}, user={}, game={}, bet={}",
        request.agentId(),
        request.userId(),
        request.gameId(),
        request.betAmount());

    SpinCommand command =
        SpinCommand.builder()
            .agentId(request.agentId())
            .userId(request.userId())
            .gameId(request.gameId())
            .betAmount(Money.of(request.betAmount()))
            .sessionId(request.sessionId())
            .trialMode(request.isTrialMode())
            .build();

    return ResponseEntity.ok(spinUseCase.execute(command));
  }

  @PostMapping("/buy-free-spins")
  public ResponseEntity<SlotResultResponse> buyFreeSpins(@Valid @RequestBody SpinRequest request) {
    log.info(
        "[API-IN] Buy Free Spins: agent={}, user={}, bet={}",
        request.agentId(),
        request.userId(),
        request.betAmount());

    BuyFeatureCommand command =
        BuyFeatureCommand.builder()
            .agentId(request.agentId())
            .userId(request.userId())
            .gameId(request.gameId())
            .sessionId(request.sessionId())
            .featureName(SlotConstants.FEATURE_FREE_SPINS)
            .betAmount(Money.of(request.betAmount()))
            .trialMode(request.isTrialMode())
            .build();

    return ResponseEntity.ok(spinUseCase.executeBuyFeature(command));
  }

  @PostMapping("/buy-hold-and-win")
  public ResponseEntity<SlotResultResponse> buyHoldAndWin(@Valid @RequestBody SpinRequest request) {
    log.info(
        "[API-IN] Buy Hold and Win: agent={}, user={}, bet={}",
        request.agentId(),
        request.userId(),
        request.betAmount());

    BuyFeatureCommand command =
        BuyFeatureCommand.builder()
            .agentId(request.agentId())
            .userId(request.userId())
            .gameId(request.gameId())
            .sessionId(request.sessionId())
            .featureName(SlotConstants.FEATURE_HOLD_AND_WIN)
            .betAmount(Money.of(request.betAmount()))
            .trialMode(request.isTrialMode())
            .build();

    return ResponseEntity.ok(spinUseCase.executeBuyHoldAndWin(command));
  }

  @GetMapping("/jackpot-history")
  public ResponseEntity<?> getJackpotHistory(
      @RequestParam String agentId, @RequestParam(defaultValue = "50") int limit) {
    List<JackpotHistory> history = jackpotHistoryPort.findByAgentId(agentId, limit);
    return ResponseEntity.ok(Map.of("data", history));
  }
}
