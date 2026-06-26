package com.doodle.service.scheduler;

import com.doodle.enums.AggregateType;
import com.doodle.enums.OutboxEventType;
import com.doodle.kafka.model.BookingEvent;
import com.doodle.kafka.producer.BookingEventProducer;
import com.doodle.model.OutboxEventEntity;
import com.doodle.repository.OutboxEventRepository;
import com.doodle.dto.OutboxEntityPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherJobTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private BookingEventProducer producer;

    @InjectMocks
    private OutboxPublisherJob job;

    // ----------------------------
    // 1. SUCCESSFUL PUBLISH
    // ----------------------------
    @Test
    void shouldPublishOutboxEventSuccessfully() {

        OutboxEventEntity event = createBaseEvent("100", 0, false);

        when(outboxEventRepository
                .findTop300ByProcessedFalseAndActiveTrueOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        job.publish();

        ArgumentCaptor<BookingEvent> captor = ArgumentCaptor.forClass(BookingEvent.class);
        verify(producer, times(1)).publish(captor.capture());

        BookingEvent sent = captor.getValue();

        assertEquals("100", sent.aggregateId());

        assertTrue(event.isProcessed());
        assertNull(event.getLastError());
    }

    // ----------------------------
    // 2. FAILURE IN KAFKA PUBLISH
    // ----------------------------
    @Test
    void shouldIncreaseRetryCountOnFailure() {

        OutboxEventEntity event = createBaseEvent("200", 0, false);

        when(outboxEventRepository
                .findTop300ByProcessedFalseAndActiveTrueOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        doThrow(new RuntimeException("Kafka down"))
                .when(producer)
                .publish(any(BookingEvent.class));

        job.publish();

        assertEquals(1, event.getRetryCount());
        assertNotNull(event.getLastError());
        assertFalse(event.isProcessed());

        verify(producer, times(1)).publish(any(BookingEvent.class));
    }

    // ----------------------------
    // 3. MAX RETRY REACHED → MARK PROCESSED
    // ----------------------------
    @Test
    void shouldMarkProcessedAfterMaxRetries() {

        OutboxEventEntity event = createBaseEvent("300", 5, false);

        when(outboxEventRepository
                .findTop300ByProcessedFalseAndActiveTrueOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        doThrow(new RuntimeException("Kafka failure"))
                .when(producer)
                .publish(any(BookingEvent.class));

        job.publish();

        assertTrue(event.isProcessed());
        assertEquals(6, event.getRetryCount());
        assertNotNull(event.getLastError());

        verify(producer, times(1)).publish(any(BookingEvent.class));
    }

    // ----------------------------
    // 4. EMPTY OUTBOX SHOULD DO NOTHING
    // ----------------------------
    @Test
    void shouldReturnEarlyWhenNoEvents() {

        when(outboxEventRepository
                .findTop300ByProcessedFalseAndActiveTrueOrderByCreatedAtAsc())
                .thenReturn(List.of());

        job.publish();

        verifyNoInteractions(producer);
    }

    // ----------------------------
    // Helper method (IMPORTANT FIX)
    // ----------------------------
    private OutboxEventEntity createBaseEvent(String id, int retryCount, boolean processed) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateId(id);
        event.setAggregateType(AggregateType.TIME_SLOT);
        event.setEventType(OutboxEventType.PENDING_RESERVATION_EVENT);
        event.setPayload(new OutboxEntityPayload(1L, null));
        event.setRetryCount(retryCount);
        event.setProcessed(processed);
        return event;
    }
}