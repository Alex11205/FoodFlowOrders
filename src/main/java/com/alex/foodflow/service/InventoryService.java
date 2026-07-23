package com.alex.foodflow.service;

import com.alex.events.InventoryEvent;
import com.alex.events.OrderCreatedEvent;
import com.alex.foodflow.dto.UpdateInventoryResponse;
import com.alex.foodflow.exceptions.InsufficientInventoryException;
import com.alex.foodflow.model.ProcessedEvent;
import com.alex.foodflow.repository.InventoryRepository;
import com.alex.foodflow.repository.ProcessedEventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;

    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 2000, multiplier = 2.0),
            exclude = {NullPointerException.class},
//            include = {RuntimeException.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt",
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR
    )
    @KafkaListener(topics = "orders-topic", groupId = "inventory-group")
    public void updateInventory(OrderCreatedEvent orderCreatedEvent) {


        Long foodId = orderCreatedEvent.foodId();

        Long orderId = orderCreatedEvent.orderId();

        int quantity = orderCreatedEvent.quantity();

        UUID eventId = orderCreatedEvent.eventId();

        if ("FAIL".equals(orderCreatedEvent.eventType())) {

            log.error("Transient error encountered while processing message");
            throw new RuntimeException("Simulated business or parsing failure");
        }

        if(!processedEventRepository.existsByEventId(eventId)) {
            int updatedInventory = inventoryRepository.updateInventory(foodId, quantity);

            ProcessedEvent processedEvent = new ProcessedEvent(
                    null,
                    eventId,
                    "InventoryService",
                    Instant.now()
            );

            processedEventRepository.save(processedEvent);

            InventoryEvent inventoryEvent = new InventoryEvent(

                    UUID.randomUUID(),
                    updatedInventory == 1 ? "InventoryReserved" : "InventoryRejected",
                    orderId,
                    foodId,
                    updatedInventory == 1,
                    LocalDateTime.now()
            );

            kafkaTemplate.send("inventory-topic", orderCreatedEvent.orderId().toString(), inventoryEvent);
        }
//        if (updatedInventory == 0) {
//            throw new InsufficientInventoryException("Insufficient inventory");
//        }


    }

    @DltHandler
    public void handleDlt(
            OrderCreatedEvent orderCreatedEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        log.error("Message {} failed all 4 attempts. Sent to Dead Letter Topic: {}. Message: {}", orderCreatedEvent, topic, errorMessage);

    }


}
