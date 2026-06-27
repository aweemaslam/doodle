package com.doodle.service.scheduler;

import com.doodle.kafka.model.BookingEvent;
import com.doodle.kafka.producer.BookingEventProducer;
import com.doodle.model.OutboxEventEntity;
import com.doodle.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * High-performance, distributed-safe Transactional Outbox background polling relay.
 * Decouples Kafka message streaming entirely from database transactional connection pools.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherJob {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final BookingEventProducer producer;

    @Scheduled(fixedDelayString = "${app.jobs.outbox-publish-delay-ms:20}", timeUnit = TimeUnit.MILLISECONDS)
    @SchedulerLock(name = "outboxPublisherJob", lockAtLeastFor = "PT2S", lockAtMostFor = "PT20S")
    public void publish() {

        List<OutboxEventEntity> pending = outboxEventRepository.findTop300ByProcessedFalseAndActiveTrueOrderByCreatedAtAsc();

        if (pending == null || pending.isEmpty()) {
            return;
        }

        List<OutboxEventEntity> updatesToCommit = new ArrayList<>();

        for (OutboxEventEntity event : pending) {
            try {
                log.info("Relaying transactional event package. Identity ID: {}, Topic Type: {}",
                        event.getId(), event.getEventType());

                producer.publish(new BookingEvent(
                        event.getAggregateId(),
                        event.getAggregateType().name(),
                        event.getEventType().name(),
                        event.getPayload()
                ));

                event.setProcessed(true);
                event.setLastError(null);
            } catch (Exception ex) {
                int retries = event.getRetryCount() == null ? 0 : event.getRetryCount();
                event.setRetryCount(retries + 1);
                event.setLastError(ex.getMessage());

                if (event.getRetryCount() >= MAX_RETRIES) {
                    log.error("Outbox event [{}] reached terminal retry threshold ({}). Marking processed for triage.",
                            event.getId(), MAX_RETRIES);
                    event.setProcessed(true);
                }

                log.error("Broker connection timeout intercepted for outbox record ID: {}, current retry index: {}",
                        event.getId(), event.getRetryCount(), ex);
            }
            updatesToCommit.add(event);
        }

         outboxEventRepository.saveAll(updatesToCommit);
    }
}
