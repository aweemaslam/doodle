package com.doodle.kafka.config;

import com.doodle.kafka.model.BookingEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * High-throughput concurrent Kafka listener container factory configuration.
 * Optimized specifically to route stream message processing via Java Virtual Threads.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BookingEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, BookingEvent> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, BookingEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        SimpleAsyncTaskExecutor virtualTaskExecutor = new SimpleAsyncTaskExecutor("kafka-vt-");
        virtualTaskExecutor.setVirtualThreads(true);
        factory.getContainerProperties().setListenerTaskExecutor(virtualTaskExecutor);

        return factory;
    }
}