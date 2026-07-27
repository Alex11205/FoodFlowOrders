package com.alex.foodfloworders.service;

import com.alex.foodfloworders.client.InventoryClient;
import com.alex.events.InventoryEvent;
import com.alex.events.OrderCreatedEvent;
import com.alex.foodfloworders.dto.PostOrderRequest;
import com.alex.foodfloworders.dto.OrderResponse;
import com.alex.foodfloworders.exceptions.NoSuchOrderException;
import com.alex.foodfloworders.model.Order;
import com.alex.foodfloworders.model.Status;
import com.alex.foodfloworders.repository.OrderRepository;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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
    private final IdempotencyCache idempotencyCache;
    private final OrderEventProcessor orderEventProcessor;

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

        kafkaTemplate.send("orders-topic", order.getId().toString(), orderCreatedEvent)
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        log.atInfo()
                                .addKeyValue("orderId", orderCreatedEvent.orderId())
                                .addKeyValue("eventId", orderCreatedEvent.eventId())
                                .addKeyValue("foodId", orderCreatedEvent.foodId())
                                .addKeyValue("status", order.getStatus())
                                .addKeyValue("topic", "orders-topic")
                                .log("Order event published");
                    } else {
                        log.atError()
                                .setCause(exception)
                                .addKeyValue("orderId", orderCreatedEvent.orderId())
                                .addKeyValue("eventId", orderCreatedEvent.eventId())
                                .addKeyValue("foodId", orderCreatedEvent.foodId())
                                .addKeyValue("status", order.getStatus())
                                .addKeyValue("topic", "orders-topic")
                                .log("Order event publishing failed");
                    }
                });

//        boolean isReserved = inventoryClient.reserveInventory(
//                postOrderRequest.foodId(),
//                postOrderRequest.quantity()
//        );


        return new OrderResponse(
                updatedOrder.getId(),
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
    @CachePut(cacheNames = "orders", key = "#inventoryEvent.orderId()")
    public OrderResponse updateOrder(InventoryEvent inventoryEvent) {
        Long orderId = inventoryEvent.orderId();
        UUID eventId = inventoryEvent.eventId();
        String consumer = "order";



        if ("FAIL".equals(inventoryEvent.eventType())) {

            log.error("Transient error encountered while processing message");
            throw new RuntimeException("Simulated business or parsing failure");
        }

        OrderEventProcessor.Result result;
        if (!idempotencyCache.wasProcessed(consumer, eventId)) {
            result = orderEventProcessor.process(inventoryEvent);
            idempotencyCache.remember(consumer, eventId);
        } else {
            result = new OrderEventProcessor.Result(
                    false,
                    orderEventProcessor.findResponse(orderId)
            );
        }

        log.atInfo()
                .addKeyValue("orderId", orderId)
                .addKeyValue("eventId", eventId)
                .addKeyValue("status", result.response().status())
                .log(result.processed()
                        ? "Order status updated"
                        : "Duplicate inventory event ignored");

        return result.response();
    }

    @DltHandler
    public void handleDlt(
            InventoryEvent inventoryEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        log.atError()
                .addKeyValue("orderId", inventoryEvent.orderId())
                .addKeyValue("eventId", inventoryEvent.eventId())
                .addKeyValue("topic", topic)
                .addKeyValue("errorMessage", errorMessage)
                .log("Inventory event exhausted retries and reached DLT");
    }

    @Cacheable(cacheNames = "orders", key = "#id")
    public OrderResponse getOrderById(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchOrderException("No Such Order"));

        return new OrderResponse(
                order.getId(),
                order.getFoodId(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
