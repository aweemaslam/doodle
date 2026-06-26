package com.doodle.repository;

import com.doodle.enums.AggregateType;
import com.doodle.enums.OutboxEventType;
import com.doodle.model.OutboxEventEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventRepositoryTest {

    // ----------------------------
    // 1. CREATE AND VALIDATE OUTBOX EVENT
    // ----------------------------
    @Test
    void shouldCreateOutboxEvent() {

        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateId("1");
        event.setAggregateType(AggregateType.TIME_SLOT);
        event.setEventType(OutboxEventType.PENDING_RESERVATION_EVENT);
        event.setProcessed(false);
        event.setActive(true);

        // Verify entity state
        assertEquals("1", event.getAggregateId());
        assertEquals(AggregateType.TIME_SLOT, event.getAggregateType());
        assertEquals(OutboxEventType.PENDING_RESERVATION_EVENT, event.getEventType());
        assertFalse(event.isProcessed());
        assertTrue(event.isActive());
    }

    // ----------------------------
    // 2. OUTBOX EVENT STATE TRANSITIONS
    // ----------------------------
    @Test
    void shouldTransitionEventStates() {

        OutboxEventEntity event = new OutboxEventEntity();
        event.setProcessed(false);
        event.setActive(true);

        // Verify initial state
        assertFalse(event.isProcessed());
        assertTrue(event.isActive());

        // Mark processed
        event.setProcessed(true);
        assertTrue(event.isProcessed());

        // Deactivate
        event.setActive(false);
        assertFalse(event.isActive());
    }

    // ----------------------------
    // 3. AGGREGATE ID AND TYPE SETTING
    // ----------------------------
    @Test
    void shouldSetAggregateIdAndType() {

        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateId("123");
        event.setAggregateType(AggregateType.TIME_SLOT);

        assertEquals("123", event.getAggregateId());
        assertEquals(AggregateType.TIME_SLOT, event.getAggregateType());
    }
}
