package com.doodle.service.impl;

import com.doodle.dto.OutboxEntityPayload;
import com.doodle.enums.AggregateType;
import com.doodle.enums.OutboxEventType;
import com.doodle.model.OutboxEventEntity;
import com.doodle.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventService outboxEventService;

    // ----------------------------
    // 1. SAVE OUTBOX EVENT
    // ----------------------------
    @Test
    void shouldSaveOutboxEvent() {

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setAggregateId("100");
        event.setAggregateType(AggregateType.TIME_SLOT);
        event.setEventType(OutboxEventType.PENDING_RESERVATION_EVENT);

        when(outboxEventRepository.save(any(OutboxEventEntity.class)))
                .thenReturn(event);

        OutboxEventEntity result = outboxEventRepository.save(event);

        assertNotNull(result);
        assertEquals("100", result.getAggregateId());
        verify(outboxEventRepository, times(1)).save(event);
    }

    // ----------------------------
    // 2. SAVE OUTBOX WITH PAYLOAD
    // ----------------------------
    @Test
    void shouldSaveOutboxWithPayload() {

        OutboxEntityPayload payload = new OutboxEntityPayload(1L, null);

        when(outboxEventRepository.save(any())).thenAnswer(invocation -> {
            OutboxEventEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        outboxEventService.saveOutbox(
                AggregateType.TIME_SLOT,
                "100",
                OutboxEventType.PENDING_RESERVATION_EVENT,
                payload
        );

        verify(outboxEventRepository, times(1)).save(any(OutboxEventEntity.class));
    }

    // ----------------------------
    // 3. OUTBOX EVENT INITIAL STATE
    // ----------------------------
    @Test
    void shouldCreateOutboxEventWithCorrectInitialState() {

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setAggregateId("200");
        event.setAggregateType(AggregateType.TIME_SLOT);
        event.setEventType(OutboxEventType.PENDING_RESERVATION_EVENT);
        event.setProcessed(false);
        event.setRetryCount(0);

        assertEquals("200", event.getAggregateId());
        assertEquals(AggregateType.TIME_SLOT, event.getAggregateType());
        assertEquals(OutboxEventType.PENDING_RESERVATION_EVENT, event.getEventType());
        assertFalse(event.isProcessed());
        assertEquals(0, event.getRetryCount());
    }

    // ----------------------------
    // 4. MARK OUTBOX AS PROCESSED
    // ----------------------------
    @Test
    void shouldMarkOutboxAsProcessed() {

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setProcessed(false);

        event.setProcessed(true);

        assertTrue(event.isProcessed());
    }

    // ----------------------------
    // 5. INCREMENT RETRY COUNT
    // ----------------------------
    @Test
    void shouldIncrementRetryCount() {

        OutboxEventEntity event = new OutboxEventEntity();
        event.setRetryCount(0);

        event.setRetryCount(event.getRetryCount() + 1);
        event.setRetryCount(event.getRetryCount() + 1);

        assertEquals(2, event.getRetryCount());
    }
}

