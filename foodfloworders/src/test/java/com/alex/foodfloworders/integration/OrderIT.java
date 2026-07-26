package com.alex.foodfloworders.integration;

import com.alex.events.InventoryEvent;
import com.alex.foodfloworders.model.Order;
import com.alex.foodfloworders.model.Status;
import com.alex.foodfloworders.repository.OrderRepository;
import com.alex.foodfloworders.repository.ProcessedEventRepository;
import com.alex.foodfloworders.service.OrderEventProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(OrderEventProcessor.class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderEventProcessor orderEventProcessor;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void reservedInventoryConfirmsPendingOrder() {
        Order order = savePendingOrder();
        UUID eventId = UUID.randomUUID();

        OrderEventProcessor.Result result = orderEventProcessor.process(
                inventoryEvent(eventId, order.getId(), true)
        );

        entityManager.flush();
        entityManager.clear();

        assertThat(result.processed()).isTrue();
        assertThat(result.response().status()).isEqualTo(Status.CONFIRMED);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(Status.CONFIRMED);
        assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
    }

    @Test
    void rejectedInventoryRejectsPendingOrder() {
        Order order = savePendingOrder();

        OrderEventProcessor.Result result = orderEventProcessor.process(
                inventoryEvent(UUID.randomUUID(), order.getId(), false)
        );

        entityManager.flush();
        entityManager.clear();

        assertThat(result.response().status()).isEqualTo(Status.REJECTED);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(Status.REJECTED);
    }

    @Test
    void duplicateInventoryEventDoesNotApplyTwice() {
        Order order = savePendingOrder();
        UUID eventId = UUID.randomUUID();

        OrderEventProcessor.Result first = orderEventProcessor.process(
                inventoryEvent(eventId, order.getId(), true)
        );
        OrderEventProcessor.Result duplicate = orderEventProcessor.process(
                inventoryEvent(eventId, order.getId(), false)
        );

        entityManager.flush();
        entityManager.clear();

        assertThat(first.processed()).isTrue();
        assertThat(duplicate.processed()).isFalse();
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(Status.CONFIRMED);
    }

    private Order savePendingOrder() {
        return orderRepository.saveAndFlush(
                new Order(null, 1L, 1, Status.PENDING, Instant.now())
        );
    }

    private InventoryEvent inventoryEvent(UUID eventId, Long orderId, boolean reserved) {
        return new InventoryEvent(
                eventId,
                reserved ? "InventoryReserved" : "InventoryRejected",
                orderId,
                1L,
                reserved,
                LocalDateTime.now()
        );
    }
}
