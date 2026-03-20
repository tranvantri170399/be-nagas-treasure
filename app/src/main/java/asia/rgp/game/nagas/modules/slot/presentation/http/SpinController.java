package asia.rgp.game.nagas.modules.slot.presentation.http;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import asia.rgp.game.nagas.modules.slot.application.dto.request.SpinCommand;
import asia.rgp.game.nagas.modules.slot.application.dto.request.BuyFeatureCommand;
import asia.rgp.game.nagas.modules.slot.presentation.dto.response.SlotResultResponse;
import asia.rgp.game.nagas.modules.slot.application.port.in.SpinUseCase;
import asia.rgp.game.nagas.modules.slot.presentation.dto.request.SpinRequest;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotConstants;
import asia.rgp.game.nagas.shared.domain.model.Money;

@Slf4j
@RestController
@RequestMapping("/api/v1/slot")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class SpinController {

    private final SpinUseCase spinUseCase;

    @PostMapping("/spin")
    public ResponseEntity<SlotResultResponse> spin(@Valid @RequestBody SpinRequest request) {
        log.info("[API-IN] Spin request: user={}, game={}, bet={}, sid={}", 
                request.userId(), request.gameId(), request.betAmount(), request.sessionId());

        SpinCommand command = SpinCommand.builder()
                .userId(request.userId())
                .gameId(request.gameId())
                .betAmount(Money.of(request.betAmount()))
                .sessionId(request.sessionId())
                .build();

        SlotResultResponse response = spinUseCase.execute(command);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/buy-free-spins")
    public ResponseEntity<SlotResultResponse> buyFreeSpins(@Valid @RequestBody SpinRequest request) {
        log.info("[API-IN] Buy Free Spins: user={}, game={}, baseBet={}", 
                request.userId(), request.gameId(), request.betAmount());

        BuyFeatureCommand command = BuyFeatureCommand.builder()
                .userId(request.userId())
                .gameId(request.gameId())
                .sessionId(request.sessionId())
                .featureName(SlotConstants.FEATURE_FREE_SPINS)
                .betAmount(Money.of(request.betAmount()))
                .build();

        SlotResultResponse response = spinUseCase.executeBuyFeature(command);

        return ResponseEntity.ok(response);
    }

    /**
     * API Mua tính năng Hold and Win
     */
    @PostMapping("/buy-hold-and-win")
    public ResponseEntity<SlotResultResponse> buyHoldAndWin(@Valid @RequestBody SpinRequest request) {
        log.info("[API-IN] Buy Hold and Win: user={}, game={}, baseBet={}", 
                request.userId(), request.gameId(), request.betAmount());

        BuyFeatureCommand command = BuyFeatureCommand.builder()
                .userId(request.userId())
                .gameId(request.gameId())
                .sessionId(request.sessionId())
                .featureName(SlotConstants.FEATURE_HOLD_AND_WIN)
                .betAmount(Money.of(request.betAmount()))
                .build();

        SlotResultResponse response = spinUseCase.executeBuyHoldAndWin(command);

        log.info("[API-OUT] Buy Hold and Win triggered for user: {}", request.userId());

        return ResponseEntity.ok(response);
    }
}