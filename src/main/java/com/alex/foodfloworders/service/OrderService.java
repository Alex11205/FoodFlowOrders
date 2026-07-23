package com.alex.foodfloworders.service;

import com.alex.foodfloworders.client.InventoryClient;
import com.alex.events.InventoryEvent;
import com.alex.events.OrderCreatedEvent;
import com.alex.foodfloworders.dto.PostOrderRequest;
import com.alex.foodfloworders.dto.OrderResponse;
import com.alex.foodfloworders.exceptions.NoSuchOrderException;
import com.alex.foodfloworders.model.Order;
import com.alex.foodfloworders.model.ProcessedEvent;
import com.alex.foodfloworders.model.Status;
import com.alex.foodfloworders.repository.OrderRepository;
import com.alex.foodfloworders.repository.ProcessedEventRepository;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
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
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;

//    private final InventoryClient inventoryClient;

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;


    public OrderResponse postOrder(PostOrderRequest postOrderRequest) {
        Order order = new Order(
                null,
                postOrderRequest.foodId(),
                postOrderRequest.quantity(),
                Status.PENDING,
                Instant.now()
        );

        Order updatedOrder = orderRepository.save(order);
        UUID eventId = UUID.randomUUID();

        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                eventId,
                "OrderCreated",
                order.getId(),
                order.getFoodId(),
                order.getQuantity(),
                LocalDateTime.now()
        );

        kafkaTemplate.send("orders-topic", order.getId().toString(), orderCreatedEvent);

//        boolean isReserved = inventoryClient.reserveInventory(
//                postOrderRequest.foodId(),
//                postOrderRequest.quantity()
//        );

        return new OrderResponse(
                updatedOrder.getFoodId(),
                updatedOrder.getQuantity(),
                updatedOrder.getStatus(),
                updatedOrder.getCreatedAt()
        );

    }

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 2000, multiplier = 2.0),
            exclude = {NullPointerException.class},
//            include = {RuntimeException.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt",
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR
    )
    @KafkaListener(topics = "inventory-topic", groupId = "order-group")
    public OrderResponse updateOrder(InventoryEvent inventoryEvent) {
        Long orderId = inventoryEvent.orderId();
        UUID eventId = inventoryEvent.eventId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchOrderException("No such user"));

        if ("FAIL".equals(inventoryEvent.eventType())) {

            log.error("Transient error encountered while processing message");
            throw new RuntimeException("Simulated business or parsing failure");
        }

        if(!processedEventRepository.existsByEventId(eventId)) {


            boolean isReserved = inventoryEvent.status();
            order.setStatus(isReserved ? Status.CONFIRMED : Status.REJECTED);

            Order updatedOrder = orderRepository.save(order);

            ProcessedEvent processedEvent = new ProcessedEvent(
                    null,
                    eventId,
                    "OrderService",
                    Instant.now()
            );

            processedEventRepository.save(processedEvent);

            return new OrderResponse(
                    updatedOrder.getFoodId(),
                    updatedOrder.getQuantity(),
                    updatedOrder.getStatus(),
                    updatedOrder.getCreatedAt()
            );

        }



        return new OrderResponse(
                order.getFoodId(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    @DltHandler
    public void handleDlt(
            InventoryEvent inventoryEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        log.error("Message {} failed all 4 attempts. Sent to Dead Letter Topic: {}. Message: {}", inventoryEvent, topic, errorMessage);

    }

    public OrderResponse getOrderById(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchOrderException("No Such Order"));

        return new OrderResponse(
                order.getFoodId(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
