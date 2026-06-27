package com.doodle.kafka.consumer;

import com.doodle.kafka.model.BookingEvent;
import com.doodle.dto.OutboxEntityPayload;
import com.doodle.service.IBookingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventKafkaConsumerTest {

    @Mock
    private IBookingsService bookingService;

    @InjectMocks
    private BookingEventKafkaConsumer consumer;

    // ----------------------------
    // 1. PENDING EVENT → BUSINESS FLOW
    // ----------------------------
    @Test
    void shouldTriggerReservationOnPendingEvent() throws Exception {

        OutboxEntityPayload payload = mock(OutboxEntityPayload.class);

        BookingEvent event = new BookingEvent(
                "100",
                "TIME_SLOT",
                "PENDING_RESERVATION_EVENT",
                payload
        );

        consumer.consumeEvent(event);

        verify(bookingService, times(1))
                .performReservation(payload);
    }

    // ----------------------------
    // 2. RESERVED EVENT → NO SERVICE CALL
    // ----------------------------
    @Test
    void shouldNotCallServiceForReservedEvent() throws Exception {

        BookingEvent event = new BookingEvent(
                "200",
                "TIME_SLOT",
                "RESERVED_EVENT",
                mock(OutboxEntityPayload.class)
        );

        consumer.consumeEvent(event);

        verifyNoInteractions(bookingService);
    }

    // ----------------------------
    // 3. UNKNOWN EVENT → SAFE IGNORE
    // ----------------------------
    @Test
    void shouldIgnoreUnknownEventGracefully() throws Exception {

        BookingEvent event = new BookingEvent(
                "300",
                "TIME_SLOT",
                "UNKNOWN_EVENT",
                mock(OutboxEntityPayload.class)
        );

        consumer.consumeEvent(event);

        verifyNoInteractions(bookingService);
    }

    // ----------------------------
    // 4. NULL PAYLOAD SAFETY
    // ----------------------------
    @Test
    void shouldHandleNullPayloadWithoutCrash() {

        BookingEvent event = new BookingEvent(
                "400",
                "TIME_SLOT",
                "PENDING_RESERVATION_EVENT",
                null
        );

        assertDoesNotThrow(() -> consumer.consumeEvent(event));
    }
}