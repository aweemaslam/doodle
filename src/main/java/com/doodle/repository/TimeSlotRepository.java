package com.doodle.repository;


import com.doodle.enums.SlotStatus;
import com.doodle.model.TimeSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlotEntity, Long> {

    /**
     * Utilized by the RedisHydrationCacheWarmer on startup to scan and 
     * seed active future slots into the Redis fast-path execution cache.
     *
     * @param status The target availability state (typically SlotStatus.FREE)
     * @param time The cut-off threshold timestamp (typically Instant.now())
     * @return A list of active, upcoming time slots.
     */
    List<TimeSlotEntity> findByStatusAndEndTimeAfter(SlotStatus status, Instant time);

    /**
     * Utilized by the CalendarController to fetch all slots for a given user 
     * within a specific time bounding box. 
     * * This query triggers the high-performance composite database index: 
     * idx_owner_timezone_time (owner_id, timezone_id, start_time, end_time).
     *
     * @param ownerEmail The unique identifier of the user calendar owner.
     * @param start The absolute UTC start window boundary.
     * @param end The absolute UTC end window boundary.
     * @return A chronologically filterable list of slots.
     */
    List<TimeSlotEntity> findByOwnerEmailAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            String ownerEmail, Instant start, Instant end);

    @Query("""
        SELECT COUNT(t) > 0 FROM TimeSlotEntity t 
        WHERE t.owner.email = :ownerId 
        AND t.startTime < :endTime 
        AND t.endTime > :startTime
    """)
    boolean existsOverlappingSlot(
            @Param("ownerId") String ownerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("""
        SELECT COUNT(t) > 0 FROM TimeSlotEntity t 
        WHERE t.owner.email = :ownerId 
        AND t.startTime < :endTime 
        AND t.endTime > :startTime 
        AND t.id != :excludeSlotId
    """)
    boolean existsOverlappingSlotExcludingId(
            @Param("ownerId") String ownerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("excludeSlotId") Long excludeSlotId
    );
}