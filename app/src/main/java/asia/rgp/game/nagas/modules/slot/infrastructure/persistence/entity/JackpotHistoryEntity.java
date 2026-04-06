package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity;

import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "jackpot_histories")
@CompoundIndex(
    name = "idx_agent_type_created",
    def = "{'agent_id': 1, 'jackpot_type': 1, 'created_at': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JackpotHistoryEntity {

  @Id private String id;

  @Indexed(unique = true)
  @Field("win_id")
  private String winId;

  @Field("agent_id")
  private String agentId;

  @Field("user_id")
  private String userId;

  @Field("username")
  private String username;

  @Field("session_id")
  private String sessionId;

  @Field("jackpot_type")
  private String jackpotType;

  @Field("amount")
  private double amount;

  @Field("created_at")
  private Instant createdAt;
}
