package com.alex.foodflow.service;

import com.alex.events.InventoryEvent;
import com.alex.events.OrderCreatedEvent;
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

@Service
@AllArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryEventProcessor inventoryEventProcessor;
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
        if ("FAIL".equals(orderCreatedEvent.eventType())) {
            log.atError()
                    .addKeyValue("orderId", orderCreatedEvent.orderId())
                    .addKeyValue("eventId", orderCreatedEvent.eventId())
                    .log("Transient error encountered while processing message");
            throw new RuntimeException("Simulated business or parsing failure");
        }

        inventoryEventProcessor.process(orderCreatedEvent)
                .ifPresentOrElse(
                        this::publishInventoryEvent,
                        () -> log.atInfo()
                                .addKeyValue("orderId", orderCreatedEvent.orderId())
                                .addKeyValue("eventId", orderCreatedEvent.eventId())
                                .log("Duplicate order event ignored")
                );
    }

    @DltHandler
    public void handleDlt(
            OrderCreatedEvent orderCreatedEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        log.atError()
                .addKeyValue("orderId", orderCreatedEvent.orderId())
                .addKeyValue("eventId", orderCreatedEvent.eventId())
                .addKeyValue("topic", topic)
                .addKeyValue("errorMessage", errorMessage)
                .log("Order event moved to dead-letter topic");
    }

    private void publishInventoryEvent(InventoryEvent inventoryEvent) {
        kafkaTemplate.send("inventory-topic", inventoryEvent.orderId().toString(), inventoryEvent)
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        log.atInfo()
                                .addKeyValue("orderId", inventoryEvent.orderId())
                                .addKeyValue("eventId", inventoryEvent.eventId())
                                .addKeyValue("foodId", inventoryEvent.foodId())
                                .addKeyValue("status", inventoryEvent.status())
                                .addKeyValue("topic", result.getRecordMetadata().topic())
                                .log("Inventory event published");
                    } else {
                        log.atError()
                                .setCause(exception)
                                .addKeyValue("orderId", inventoryEvent.orderId())
                                .addKeyValue("eventId", inventoryEvent.eventId())
                                .addKeyValue("foodId", inventoryEvent.foodId())
                                .addKeyValue("status", inventoryEvent.status())
                                .addKeyValue("topic", "inventory-topic")
                                .log("Inventory event publishing failed");
                    }
                });
    }
}
