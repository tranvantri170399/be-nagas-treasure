package asia.rgp.game.nagas.shared.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Getter;
import lombok.Setter;

/**
 * Common MongoDB document fields shared across aggregates.
 */
@Getter
@Setter
public abstract class BaseDocument {

    @Id
    private String id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant deletedAt;
}
