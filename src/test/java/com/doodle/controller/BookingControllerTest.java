package com.doodle.controller;

import com.doodle.dto.BookingRequest;
import com.doodle.dto.BookingResponse;
import com.doodle.enums.SlotStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BookingControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // ----------------------------
    // 1. BOOKING REQUEST CREATION
    // ----------------------------
    @Test
    void shouldCreateBookingRequest() {

        BookingRequest request = new BookingRequest(
                "Team Sync",
                "Discuss roadmap",
                Set.of("a@b.com", "c@d.com"),
                "organizer@example.com"
        );

        assertEquals("Team Sync", request.title());
        assertEquals("Discuss roadmap", request.description());
        assertEquals(2, request.participants().size());
        assertTrue(request.participants().contains("a@b.com"));
    }

    // ----------------------------
    // 2. BOOKING RESPONSE CREATION
    // ----------------------------
    @Test
    void shouldCreateBookingResponse() {

        BookingResponse response = new BookingResponse(
                SlotStatus.PENDING_RESERVATION,
                "Booking initiated, awaiting confirmation"
        );

        assertEquals(SlotStatus.PENDING_RESERVATION, response.status());
        assertNotNull(response.message());
        assertTrue(response.message().contains("Booking"));
    }

    // ----------------------------
    // 3. BOOKING REQUEST JSON SERIALIZATION
    // ----------------------------
    @Test
    void shouldSerializeBookingRequestToJson() throws Exception {

        BookingRequest request = new BookingRequest(
                "Team Meeting",
                "Q4 Planning",
                Set.of("alice@example.com"),
                "host@example.com"
        );

        String json = objectMapper.writeValueAsString(request);
        assertNotNull(json);
        assertTrue(json.contains("Team Meeting"));
        assertTrue(json.contains("alice@example.com"));
    }

    // ----------------------------
    // 4. BOOKING REQUEST JSON DESERIALIZATION
    // ----------------------------
    @Test
    void shouldDeserializeBookingRequestFromJson() throws Exception {

        String json = "{\"title\":\"Standup\",\"description\":\"Daily standup\",\"participants\":[\"dev@example.com\"],\"ownerId\":\"manager@example.com\"}";

        BookingRequest request = objectMapper.readValue(json, BookingRequest.class);

        assertEquals("Standup", request.title());
        assertEquals("Daily standup", request.description());
        assertEquals(1, request.participants().size());
    }

    // ----------------------------
    // 5. BOOKING WITH MULTIPLE PARTICIPANTS
    // ----------------------------
    @Test
    void shouldHandleMultipleParticipants() {

        Set<String> participants = Set.of("alice@test.com", "bob@test.com", "charlie@test.com");
        BookingRequest request = new BookingRequest(
                "Group Meeting",
                "Team discussion",
                participants,
                "organizer@test.com"
        );

        assertEquals(3, request.participants().size());
        assertTrue(request.participants().containsAll(participants));
    }
}
