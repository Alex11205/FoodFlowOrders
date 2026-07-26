package com.alex.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        String eventType,
        Long orderId,
        Long foodId,
        int quantity,
        LocalDateTime occurredAt
) {}
