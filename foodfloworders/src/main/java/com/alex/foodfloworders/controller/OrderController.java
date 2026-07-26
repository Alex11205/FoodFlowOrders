package com.alex.foodfloworders.controller;

import com.alex.foodfloworders.dto.OrderResponse;
import com.alex.foodfloworders.dto.PostOrderRequest;
import com.alex.foodfloworders.service.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody PostOrderRequest postOrderRequest) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.postOrder(postOrderRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(orderService.getOrderById(id));
    }

}
