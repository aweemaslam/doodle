package com.doodle.model;

import com.doodle.enums.SlotStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "time_slots")
@Getter
@Setter
public class TimeSlotEntity extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_slot_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email", nullable = false, unique = true)
    private UserEntity owner;

    @Column(name = "start_time", nullable = false)
    private Instant startTime; // Always normalized to UTC internally

    @Column(name = "end_time", nullable = false)
    private Instant endTime;   // Always normalized to UTC internally

    @Column(name = "timezone_id", nullable = false)
    private String timezoneId; // Captures original IANA ID (e.g., "Europe/London")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status = SlotStatus.FREE;
}