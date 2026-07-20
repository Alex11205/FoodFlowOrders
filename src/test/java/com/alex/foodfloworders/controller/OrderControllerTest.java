package com.alex.foodfloworders.controller;

import com.alex.foodfloworders.dto.OrderResponse;
import com.alex.foodfloworders.dto.PostOrderRequest;
import com.alex.foodfloworders.model.Status;
import com.alex.foodfloworders.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.when;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createOrder_shouldReturnOk() throws Exception {

        PostOrderRequest postOrderRequest= new PostOrderRequest(1L, 1);

        OrderResponse orderResponse = new OrderResponse(
                1L,
                1,
                Status.CONFIRMED,
                Instant.now()
        );

        when(orderService.postOrder(postOrderRequest)).thenReturn(orderResponse);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postOrderRequest)))
                .andExpect(status().isCreated());

    }


    @Test
    void createOrder_shouldThrowBadRequest_WhenRequestBodyNotExistOrJsonIsMalformed() throws Exception {


        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString("str")))
                .andExpect(status().isBadRequest());

    }

    @Test
    void createOrder_shouldThrowBadRequest_WhenValidationFails() throws Exception {

        PostOrderRequest postOrderRequest = new PostOrderRequest(1L, 10000);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed: Quantity cannot be more than 100"));

    }
}
