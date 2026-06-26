package com.doodle.kafka.consumer;


import com.doodle.kafka.model.BookingEvent;
import com.doodle.repository.MeetingRepository;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.service.IMeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventKafkaConsumer {
    private final IMeetingService meetingService;

    @KafkaListener(topics = "${spring.kafka.topic.book-slot-event}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeEvent(BookingEvent bookingEvent) throws Exception {

        log.info("Received event: {}", bookingEvent);

        switch (bookingEvent.eventType()) {
            case "PENDING_RESERVATION_EVENT" -> meetingService.performReservation(bookingEvent.payload());
            case "RESERVED_EVENT" -> generateNotification(bookingEvent);
        }
    }

    void generateNotification(BookingEvent bookingEvent) {
        log.info("Slot reserved and call generate notification method for event: {}", bookingEvent);
    }
}