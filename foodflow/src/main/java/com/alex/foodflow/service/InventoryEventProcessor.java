package com.alex.foodflow.service;

import com.alex.events.InventoryEvent;
import com.alex.events.OrderCreatedEvent;
import com.alex.foodflow.repository.InventoryRepository;
import com.alex.foodflow.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryEventProcessor {

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public Optional<InventoryEvent> process(OrderCreatedEvent orderCreatedEvent) {
        int claimed = processedEventRepository.claim(
                orderCreatedEvent.eventId(),
                "InventoryService"
        );
        if (claimed == 0) {
            return Optional.empty();
        }

        int updatedInventory = inventoryRepository.updateInventory(
                orderCreatedEvent.foodId(),
                orderCreatedEvent.quantity()
        );

        return Optional.of(new InventoryEvent(
                UUID.randomUUID(),
                updatedInventory == 1 ? "InventoryReserved" : "InventoryRejected",
                orderCreatedEvent.orderId(),
                orderCreatedEvent.foodId(),
                updatedInventory == 1,
                LocalDateTime.now()
        ));
    }
}
