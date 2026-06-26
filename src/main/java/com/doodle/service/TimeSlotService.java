package com.doodle.service;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.enums.SlotStatus;

import java.util.List;

public interface TimeSlotService {
    TimeSlotResponse createSlot(SlotRequest request);
    List<TimeSlotResponse> createBulkSlots(BulkSlotRequest request);
    TimeSlotResponse modifySlot(Long id, SlotRequest request);
    void changeStatus(Long id, SlotStatus status);
    void deleteSlot(Long id);
}