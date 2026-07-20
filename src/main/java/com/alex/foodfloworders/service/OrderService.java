package com.alex.foodfloworders.service;

import com.alex.foodfloworders.client.InventoryClient;
import com.alex.foodfloworders.dto.PostOrderRequest;
import com.alex.foodfloworders.dto.OrderResponse;
import com.alex.foodfloworders.exceptions.NoSuchOrderException;
import com.alex.foodfloworders.model.Order;
import com.alex.foodfloworders.model.Status;
import com.alex.foodfloworders.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public OrderResponse postOrder(PostOrderRequest postOrderRequest) {
        Order order = new Order(
                null,
                postOrderRequest.foodId(),
                postOrderRequest.quantity(),
                Status.PENDING,
                Instant.now()
        );

        orderRepository.save(order);

        boolean isReserved = inventoryClient.reserveInventory(
                postOrderRequest.foodId(),
                postOrderRequest.quantity()
        );

        order.setStatus(isReserved ? Status.CONFIRMED : Status.REJECTED);

        Order updatedOrder = orderRepository.save(order);


        return new OrderResponse(
                updatedOrder.getFoodId(),
                updatedOrder.getQuantity(),
                updatedOrder.getStatus(),
                updatedOrder.getCreatedAt()
        );

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
