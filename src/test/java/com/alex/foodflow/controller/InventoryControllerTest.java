package com.alex.foodflow.controller;


import com.alex.foodflow.dto.OrderRequest;
import com.alex.foodflow.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
public class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

//    @Test
//    void updateInventory_shouldReturnOk() throws Exception {
//
//        OrderRequest orderRequest = new OrderRequest(1);
//
//        when(inventoryService.updateInventory(any(Long.class), any(Integer.class))).thenReturn(1);
//
//        mockMvc.perform(post("/inventory/1/reserve")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(orderRequest)))
//                .andExpect(status().isOk());
//
//    }
//
//
//    @Test
//    void updateInventory_shouldThrowBadRequest_WhenRequestBodyNotExistOrJsonIsMalformed() throws Exception {
//
//
//        mockMvc.perform(post("/inventory/1/reserve")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString("str")))
//                .andExpect(status().isBadRequest());
//
//    }
//
//    @Test
//    void updateInventory_shouldThrowBadRequest_WhenValidationFails() throws Exception {
//
//        OrderRequest orderRequest = new OrderRequest(10000);
//
//        mockMvc.perform(post("/inventory/1/reserve")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(orderRequest)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.message").value("Validation failed: Quantity cannot be more than 100"));
//
//    }
}
