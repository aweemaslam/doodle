package com.doodle.kafka.consumer;


import com.doodle.kafka.model.BookingEvent;
import com.doodle.service.IBookingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventKafkaConsumer {
    private final IBookingsService meetingService;

    @KafkaListener(topics = "${spring.kafka.topic.book-slot-event:resere-slot-topic}",
            groupId = "${spring.kafka.consumer.group-id:booking-persistence-group}")
    @Transactional
    public void consumeEvent(BookingEvent bookingEvent) throws Exception {

        log.info("Received event: {}", bookingEvent);

        switch (bookingEvent.eventType()) {
            case "PENDING_RESERVATION_EVENT" -> meetingService.performReservation(bookingEvent.payload());
            case "RESERVED_EVENT" -> generateNotification(bookingEvent);
            default -> log.warn("Unmatched event type token bypassed processing chain: {}", bookingEvent.eventType());
        }
    }

    private void generateNotification(BookingEvent bookingEvent) {
        log.info("Slot transaction finalized. Triggering notification handler for event ID: {}", bookingEvent.aggregateId());
    }
}