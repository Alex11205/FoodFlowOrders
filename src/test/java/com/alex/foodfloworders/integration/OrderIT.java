package com.alex.foodflow.integration;

import com.alex.foodflow.dto.OrderRequest;
import com.alex.foodflow.model.Inventory;
import com.alex.foodflow.repository.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest()
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Sql(
        statements = {
                "TRUNCATE TABLE inventory RESTART IDENTITY CASCADE"
        },
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
public class InventoryIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");
    static {
        postgres.start();
    }

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldSubtractQuantity() throws Exception {

        int quantity = 3;
        int quantityBefore = 100;
        int quantityAfter = 97;
        String foodName = "foodName";

        Inventory inventory = new Inventory(foodName, quantityBefore);
        Inventory newInventory = inventoryRepository.save(inventory);
        OrderRequest orderRequest = new OrderRequest(quantity);
        Long id = newInventory.getId();

        String url = "/inventory/" + id + "/reserve";
        mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk());

        Inventory update = inventoryRepository.findById(id).orElseThrow();
        assertEquals(quantityAfter, update.getAvailableQuantity());
    }


}
