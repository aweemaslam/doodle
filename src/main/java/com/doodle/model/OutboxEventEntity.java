package com.doodle.model;

import com.doodle.dto.OutboxEntityPayload;
import com.doodle.enums.AggregateType;
import com.doodle.enums.OutboxEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

/**
 * Transactional Outbox event log entry entity.
 * Captures systematic changes to aggregates atomically within the same database transaction.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEventEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "outbox_event_id", nullable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false)
    private AggregateType aggregateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private OutboxEntityPayload payload;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
