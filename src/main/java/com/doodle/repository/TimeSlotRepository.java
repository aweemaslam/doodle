package com.doodle.repository;

import com.doodle.enums.SlotStatus;
import com.doodle.model.TimeSlotEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data access repository managing availability scheduling records.
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlotEntity, Long> {

    List<TimeSlotEntity> findByStatusAndEndTimeAfterAndActiveTrue(SlotStatus status, Instant time);

    Page<TimeSlotEntity> findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualAndActiveTrue(
            String ownerEmail, Instant start, Instant end, Pageable pageable);

    Page<TimeSlotEntity> findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualAndStatusAndActiveTrue(
            String ownerEmail, Instant start, Instant end, SlotStatus status, Pageable pageable);

    @Query("""
        SELECT COUNT(t) > 0 FROM TimeSlotEntity t 
        WHERE t.owner.email = :ownerId 
        AND t.startTime <= :endTime 
        AND t.endTime >= :startTime
        AND t.active = true
    """)
    boolean existsOverlappingSlot(
            @Param("ownerId") String ownerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("""
        SELECT COUNT(t) > 0 FROM TimeSlotEntity t 
        WHERE t.owner.email = :ownerId 
        AND t.startTime <= :endTime 
        AND t.endTime >= :startTime 
        AND t.id != :excludeSlotId
        AND t.active = true
    """)
    boolean existsOverlappingSlotExcludingId(
            @Param("ownerId") String ownerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("excludeSlotId") Long excludeSlotId
    );

    Optional<TimeSlotEntity> findByIdAndActiveTrue(Long id);

    boolean existsByIdAndActiveTrue(long id);
}