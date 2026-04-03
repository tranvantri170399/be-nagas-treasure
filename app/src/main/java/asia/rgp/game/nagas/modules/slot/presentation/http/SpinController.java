package asia.rgp.game.nagas.modules.slot.presentation.http;

import asia.rgp.game.nagas.modules.slot.application.dto.request.BuyFeatureCommand;
import asia.rgp.game.nagas.modules.slot.application.dto.request.SpinCommand;
import asia.rgp.game.nagas.modules.slot.application.port.in.SpinUseCase;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.modules.slot.domain.service.JackpotService;
import asia.rgp.game.nagas.modules.slot.presentation.dto.request.SpinRequest;
import asia.rgp.game.nagas.modules.slot.presentation.dto.response.SlotResultResponse;
import asia.rgp.game.nagas.shared.domain.model.Money;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/slot")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SpinController {

  private final SpinUseCase spinUseCase;
  private final JackpotService jackpotService;

  @GetMapping("/init")
  public ResponseEntity<SlotResultResponse> initGame(
      @RequestParam String userId, @RequestParam String gameId, @RequestParam String sessionId) {

    log.info("[API-IN] Init Game (Unified): user={}, game={}, sid={}", userId, gameId, sessionId);

    SlotResultResponse response = spinUseCase.getInitialState(userId, gameId, sessionId);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/jackpot-pools")
  public ResponseEntity<?> getJackpotPools() {
    return ResponseEntity.ok(Map.of("data", jackpotService.getAllPools()));
  }

  @PostMapping("/spin")
  public ResponseEntity<SlotResultResponse> spin(@Valid @RequestBody SpinRequest request) {
    log.info(
        "[API-IN] Spin request: user={}, game={}, bet={}",
        request.userId(),
        request.gameId(),
        request.betAmount());

    SpinCommand command =
        SpinCommand.builder()
            .userId(request.userId())
            .gameId(request.gameId())
            .betAmount(Money.of(request.betAmount()))
            .sessionId(request.sessionId())
            .build();

    return ResponseEntity.ok(spinUseCase.execute(command));
  }

  @PostMapping("/buy-free-spins")
  public ResponseEntity<SlotResultResponse> buyFreeSpins(@Valid @RequestBody SpinRequest request) {
    log.info("[API-IN] Buy Free Spins: user={}, bet={}", request.userId(), request.betAmount());

    BuyFeatureCommand command =
        BuyFeatureCommand.builder()
            .userId(request.userId())
            .gameId(request.gameId())
            .sessionId(request.sessionId())
            .featureName(SlotConstants.FEATURE_FREE_SPINS)
            .betAmount(Money.of(request.betAmount()))
            .build();

    return ResponseEntity.ok(spinUseCase.executeBuyFeature(command));
  }

  @PostMapping("/buy-hold-and-win")
  public ResponseEntity<SlotResultResponse> buyHoldAndWin(@Valid @RequestBody SpinRequest request) {
    log.info("[API-IN] Buy Hold and Win: user={}, bet={}", request.userId(), request.betAmount());

    BuyFeatureCommand command =
        BuyFeatureCommand.builder()
            .userId(request.userId())
            .gameId(request.gameId())
            .sessionId(request.sessionId())
            .featureName(SlotConstants.FEATURE_HOLD_AND_WIN)
            .betAmount(Money.of(request.betAmount()))
            .build();

    return ResponseEntity.ok(spinUseCase.executeBuyHoldAndWin(command));
  }
}
