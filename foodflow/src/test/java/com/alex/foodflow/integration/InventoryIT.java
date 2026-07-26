package com.alex.foodflow.integration;

import com.alex.events.InventoryEvent;
import com.alex.events.OrderCreatedEvent;
import com.alex.foodflow.model.Inventory;
import com.alex.foodflow.repository.InventoryRepository;
import com.alex.foodflow.repository.ProcessedEventRepository;
import com.alex.foodflow.service.InventoryEventProcessor;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(InventoryEventProcessor.class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InventoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InventoryEventProcessor inventoryEventProcessor;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void availableStockProducesReservedEventAndDecrementsInventory() {
        Inventory inventory = inventoryRepository.saveAndFlush(new Inventory("Pizza", 10));
        UUID eventId = UUID.randomUUID();

        Optional<InventoryEvent> result = inventoryEventProcessor.process(
                orderEvent(eventId, inventory.getId(), 3)
        );

        entityManager.flush();
        entityManager.clear();

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().status()).isTrue();
        assertThat(result.orElseThrow().eventType()).isEqualTo("InventoryReserved");
        assertThat(inventoryRepository.findById(inventory.getId()).orElseThrow().getAvailableQuantity())
                .isEqualTo(7);
        assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
    }

    @Test
    void insufficientStockProducesRejectedEventWithoutChangingInventory() {
        Inventory inventory = inventoryRepository.saveAndFlush(new Inventory("Pasta", 2));

        Optional<InventoryEvent> result = inventoryEventProcessor.process(
                orderEvent(UUID.randomUUID(), inventory.getId(), 3)
        );

        entityManager.flush();
        entityManager.clear();

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().status()).isFalse();
        assertThat(result.orElseThrow().eventType()).isEqualTo("InventoryRejected");
        assertThat(inventoryRepository.findById(inventory.getId()).orElseThrow().getAvailableQuantity())
                .isEqualTo(2);
    }

    @Test
    void duplicateOrderEventDoesNotReserveStockTwice() {
        Inventory inventory = inventoryRepository.saveAndFlush(new Inventory("Salad", 10));
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = orderEvent(eventId, inventory.getId(), 3);

        Optional<InventoryEvent> first = inventoryEventProcessor.process(event);
        Optional<InventoryEvent> duplicate = inventoryEventProcessor.process(event);

        entityManager.flush();
        entityManager.clear();

        assertThat(first).isPresent();
        assertThat(duplicate).isEmpty();
        assertThat(inventoryRepository.findById(inventory.getId()).orElseThrow().getAvailableQuantity())
                .isEqualTo(7);
    }

    private OrderCreatedEvent orderEvent(UUID eventId, Long foodId, int quantity) {
        return new OrderCreatedEvent(
                eventId,
                "OrderCreated",
                100L,
                foodId,
                quantity,
                LocalDateTime.now()
        );
    }
}
