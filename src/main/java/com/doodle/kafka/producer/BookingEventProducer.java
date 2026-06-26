package com.doodle.kafka.producer;

import com.doodle.kafka.model.BookingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @Value("${spring.kafka.topic.book-slot-event}")
    private String topic;

    public void publish(BookingEvent event) {
        kafkaTemplate.send(topic, event.aggregateId(), event);
    }
}