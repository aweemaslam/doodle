package com.doodle.repository;

import com.doodle.model.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access repository for managing finalized meeting allocations.
 * Acts as the primary database guard to prevent duplicate schedule slots bookings.
 */
@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    /**
     * Checks if an active booking is already bound to a specific time slot ID.
     * Utilized as a fail-fast database layer constraint check before scheduling.
     *
     * @param id The target TimeSlot primary key ID token.
     * @return True if an active meeting is already attached to this slot window.
     */
    boolean existsByTimeSlotIdAndActiveTrue(Long id);
}