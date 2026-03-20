package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;
import java.util.List;

@Document(collection = "slot_histories")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SlotHistoryEntity {
    
    @Id
    private String roundId;

    @Field("parent_round_id")
    private String parentRoundId;

    private int round;

    @Field("sub_round")
    private int subRound;

    @Field("user_id")
    private String userId;

    @Field("game_id")
    private String gameId;

    @Field("session_id")
    private String sessionId;
    
    @Field("total_bet")
    private long totalBet;

    @Field("display_bet")
    private long displayBet;

    @Field("total_win")
    private long totalWin;

    @Field("balance_after")
    private long balanceAfter;

    @Field("screen_data")
    private int[][] screenData;

    @Field("win_data")
    private Object winData;

    private String type; // "BASE", "FREE", "BUY"

    @Field("created_at")
    private Instant createdAt;
}