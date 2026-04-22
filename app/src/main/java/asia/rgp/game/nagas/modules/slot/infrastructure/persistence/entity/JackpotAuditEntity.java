package asia.rgp.game.nagas.modules.slot.infrastructure.persistence.entity;

import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Audit log for every jackpot state transition. Uses @Version for optimistic locking — if two
 * threads try to update the same audit record concurrently, one will get
 * OptimisticLockingFailureException.
 *
 * <p>State machine: CLAIMED → PAID (happy path) CLAIMED → FAILED (wallet credit failed)
 */
@Document(collection = "jackpot_audits")
@CompoundIndex(
    name = "idx_agent_tier_created",
    def = "{'agent_id': 1, 'tier': 1, 'created_at': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JackpotAuditEntity {

  @Id private String id;

  @Version private Long version;

  @Indexed(unique = true)
  @Field("win_id")
  private String winId;

  @Field("agent_id")
  private String agencyId;

  @Field("user_id")
  private String userId;

  @Field("session_id")
  private String sessionId;

  @Field("tier")
  private String tier;

  @Field("amount")
  private double amount;

  @Field("pool_before")
  private double poolBefore;

  @Field("pool_after_reset")
  private double poolAfterReset;

  /** CLAIMED, PAID, FAILED */
  @Field("status")
  private String status;

  @Field("hit_arrow")
  private boolean hitArrow;

  @Field("is_near_miss")
  private boolean isNearMiss;

  @Field("created_at")
  private Instant createdAt;

  @Field("paid_at")
  private Instant paidAt;

  @Field("error_message")
  private String errorMessage;
}
