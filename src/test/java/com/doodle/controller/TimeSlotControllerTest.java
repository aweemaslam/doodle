package com.doodle.controller;

import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.enums.SlotStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TimeSlotControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // ----------------------------
    // 1. SLOT REQUEST DTO VALIDATION
    // ----------------------------
    @Test
    void shouldCreateSlotRequest() {

        SlotRequest request = new SlotRequest(
                "user-1",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "UTC"
        );

        assertEquals("user-1", request.ownerId());
        assertNotNull(request.startTime());
        assertNotNull(request.endTime());
        assertEquals("UTC", request.timezoneId());
    }

    // ----------------------------
    // 2. SLOT RESPONSE DTO CREATION
    // ----------------------------
    @Test
    void shouldCreateSlotResponse() {

        TimeSlotResponse response = new TimeSlotResponse(
                1L,
                "user-1",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "UTC",
                SlotStatus.FREE
        );

        assertEquals(1L, response.id());
        assertEquals(SlotStatus.FREE, response.status());
        assertEquals("UTC", response.timezoneId());
    }

    // ----------------------------
    // 3. BULK SLOT REQUEST DTO
    // ----------------------------
    @Test
    void shouldCreateBulkSlotRequest() {

        BulkSlotRequest request = new BulkSlotRequest(
                "user-1",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                5,
                "UTC"
        );

        assertEquals("user-1", request.ownerId());
        assertEquals(5, request.numberOfSlots());
        assertNotNull(request.startTime());
        assertNotNull(request.endTime());
        assertEquals("UTC", request.timezoneId());
    }

    // ----------------------------
    // 4. DTO JSON SERIALIZATION
    // ----------------------------
    @Test
    void shouldSerializeSlotRequestToJson() throws Exception {

        SlotRequest request = new SlotRequest(
                "test@example.com",
                Instant.parse("2024-01-01T10:00:00Z"),
                Instant.parse("2024-01-01T11:00:00Z"),
                "UTC"
        );

        String json = objectMapper.writeValueAsString(request);
        assertNotNull(json);
        assertTrue(json.contains("test@example.com"));
        assertTrue(json.contains("UTC"));
    }

    // ----------------------------
    // 5. DTO JSON DESERIALIZATION
    // ----------------------------
    @Test
    void shouldDeserializeSlotRequestFromJson() throws Exception {

        String json = "{\"ownerId\":\"test@example.com\",\"startTime\":\"2024-01-01T10:00:00Z\",\"endTime\":\"2024-01-01T11:00:00Z\",\"timezoneId\":\"UTC\"}";

        SlotRequest request = objectMapper.readValue(json, SlotRequest.class);

        assertEquals("test@example.com", request.ownerId());
        assertEquals("UTC", request.timezoneId());
        assertNotNull(request.startTime());
        assertNotNull(request.endTime());
    }
}
