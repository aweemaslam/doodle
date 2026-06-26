package com.doodle.kafka.producer;

import com.doodle.kafka.model.BookingEvent;
import com.doodle.dto.OutboxEntityPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventProducerTest {

    @Mock
    private KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @InjectMocks
    private BookingEventProducer producer;

    // ----------------------------
    // 1. VERIFY KAFKA SEND
    // ----------------------------
    @Test
    void shouldPublishEventToKafka() {

        // inject topic manually (since @Value is not loaded in unit test)
        ReflectionTestUtils.setField(producer, "topic", "book-slot-event");

        BookingEvent event = new BookingEvent(
                "123",
                "TIME_SLOT",
                "PENDING_RESERVATION_EVENT",
                new OutboxEntityPayload(1L, null)
        );

        producer.publish(event);

        verify(kafkaTemplate, times(1))
                .send(eq("book-slot-event"), eq("123"), eq(event));
    }

    // ----------------------------
    // 2. VERIFY NO MODIFICATION OF EVENT
    // ----------------------------
    @Test
    void shouldNotMutateEventBeforeSending() {

        ReflectionTestUtils.setField(producer, "topic", "book-slot-event");

        BookingEvent event = new BookingEvent(
                "999",
                "TIME_SLOT",
                "RESERVED_EVENT",
                new OutboxEntityPayload(2L, null)
        );

        producer.publish(event);

        verify(kafkaTemplate).send(anyString(), eq("999"), eq(event));
    }
}