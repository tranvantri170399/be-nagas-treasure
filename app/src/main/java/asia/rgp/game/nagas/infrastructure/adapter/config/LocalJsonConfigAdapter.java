package asia.rgp.game.nagas.infrastructure.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import asia.rgp.game.nagas.modules.slot.application.port.out.GameConfigPort;
import asia.rgp.game.nagas.modules.slot.domain.model.SlotGameConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalJsonConfigAdapter implements GameConfigPort {

    private final ObjectMapper objectMapper;
    private final Map<String, SlotGameConfig> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:games/*.json");

        for (Resource resource : resources) {
            try {
                SlotGameConfig config = objectMapper.readValue(resource.getInputStream(), SlotGameConfig.class);
                configCache.put(config.gameId(), config);
                log.info("[Config] Loaded game: {}", config.gameId());
            } catch (Exception e) {
                log.error("[Config] Failed to load {}: {}", resource.getFilename(), e.getMessage());
            }
        }
    }

    @Override
    public Optional<SlotGameConfig> findByGameId(String gameId) {
        return Optional.ofNullable(configCache.get(gameId));
    }
}