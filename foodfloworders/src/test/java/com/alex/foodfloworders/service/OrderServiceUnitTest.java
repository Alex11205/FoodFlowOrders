package com.alex.foodfloworders.service;

import com.alex.foodfloworders.client.InventoryClient;
import com.alex.foodfloworders.dto.OrderResponse;
import com.alex.foodfloworders.dto.PostOrderRequest;
import com.alex.foodfloworders.model.Order;
import com.alex.foodfloworders.model.Status;
import com.alex.foodfloworders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderyRepository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private OrderService orderService;
//
//    @Test
//    void postOrder_shouldReturnOrder() {
//
//        Long foodId = 1L;
//        int quantity = 1;
//
//        PostOrderRequest postOrderRequest = new PostOrderRequest(
//                foodId,
//                quantity
//        );
//
//        Order order = new Order(
//                null,
//                foodId,
//                quantity,
//                Status.PENDING,
//                Instant.now()
//        );
//
//
//        when(orderyRepository.save(any())).thenReturn(order);
//        when(inventoryClient.reserveInventory(any(Long.class), any(Integer.class))).thenReturn(true);
//
//        order.setStatus(Status.CONFIRMED);
//
//        OrderResponse orderResponse = new OrderResponse(
//                order.getFoodId(),
//                order.getQuantity(),
//                order.getStatus(),
//                order.getCreatedAt()
//        );
//
//        assertEquals(orderResponse, orderService.postOrder(postOrderRequest));
//    }

}
