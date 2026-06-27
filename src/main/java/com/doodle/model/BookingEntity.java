package com.doodle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

/**
 * Core business domain entity representing a finalized meeting reservation
 * linked directly to a cleared parent time slot window.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
public class BookingEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false, unique = true)
    private TimeSlotEntity timeSlot;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "booking_participants",
            joinColumns = @JoinColumn(name = "booking_id")
    )
    @Column(name = "participant_id")
    private Set<String> participants;

}
