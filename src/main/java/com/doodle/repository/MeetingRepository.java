package com.doodle.repository;


import com.doodle.enums.SlotStatus;
import com.doodle.model.MeetingEntity;
import com.doodle.model.TimeSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, Long> {

    boolean existsByTimeSlotId(Long aLong);
}