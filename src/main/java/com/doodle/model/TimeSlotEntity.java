package com.doodle.model;

import com.doodle.enums.SlotStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/**
 * Core business domain entity tracking allocated availability windows.
 * Maps to optimized composite index configurations to allow high-concurrency calendar lookups.
 */
@Entity
@Table(name = "time_slots")
@Getter
@Setter
public class TimeSlotEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_slot_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "timezone_id", nullable = false, length = 64)
    private String timezoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SlotStatus status = SlotStatus.FREE;
}