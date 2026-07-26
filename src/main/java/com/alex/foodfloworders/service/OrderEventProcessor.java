package com.alex.foodfloworders.service;

import com.alex.events.InventoryEvent;
import com.alex.foodfloworders.dto.OrderResponse;
import com.alex.foodfloworders.exceptions.NoSuchOrderException;
import com.alex.foodfloworders.model.Order;
import com.alex.foodfloworders.model.Status;
import com.alex.foodfloworders.repository.OrderRepository;
import com.alex.foodfloworders.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderEventProcessor {

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public Result process(InventoryEvent inventoryEvent) {
        int claimed = processedEventRepository.claim(
                inventoryEvent.eventId(),
                "OrderService"
        );

        Order order = findOrder(inventoryEvent.orderId());
        if (claimed == 0) {
            return new Result(false, toResponse(order));
        }

        order.setStatus(inventoryEvent.status() ? Status.CONFIRMED : Status.REJECTED);
        orderRepository.save(order);

        return new Result(true, toResponse(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse findResponse(Long orderId) {
        return toResponse(findOrder(orderId));
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchOrderException("No such order"));
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getFoodId(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    public record Result(boolean processed, OrderResponse response) {
    }
}
