package com.alex.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryEvent(
        UUID eventId,
        String eventType,
        Long orderId,
        Long foodId,
        boolean status,
        LocalDateTime occurredAt
) {}
