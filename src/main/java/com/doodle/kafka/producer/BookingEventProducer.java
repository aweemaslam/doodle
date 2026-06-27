package com.doodle.kafka.producer;

import com.doodle.kafka.model.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @Value("${spring.kafka.topic.book-slot-event:resere-slot-topic}")
    private String topic;

    public void publish(BookingEvent event) {
        log.info("Dispatching event to Kafka topic [{}]. AggregateId: {}, EventType: {}",
                topic, event.aggregateId(), event.eventType());

        kafkaTemplate.send(topic, event.aggregateId(), event);
    }
}