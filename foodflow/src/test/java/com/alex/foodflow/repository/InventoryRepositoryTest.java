package com.alex.foodflow.repository;

import com.alex.foodflow.model.Inventory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
//import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class InventoryRepositoryTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();;
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void updateInventory_shouldSubtractQuantity() {
        int quantityBefore = 100;
        int quantity = 3;
        int quantityAfter = 97;
        String foodName = "foodName";

        Inventory inventory = new Inventory(null, foodName, quantityBefore);

        testEntityManager.persistAndFlush(inventory);

        Long id = inventory.getId();

        int rows = inventoryRepository.updateInventory(id, quantity);
        testEntityManager.clear();
        Inventory newInventory = testEntityManager.find(Inventory.class, id);
        assertEquals(quantityAfter, newInventory.getAvailableQuantity());
        assertEquals(1, rows);
    }

    @Test
    void updateInventory_shouldNotSubtractQuantity_WhenAvailableQuantityIsInsufficient() {
        int quantityBefore = 100;
        int quantity = 300;
        int quantityAfter = 100;
        String foodName = "foodName";

        Inventory inventory = new Inventory(null, foodName, quantityBefore);

        testEntityManager.persistAndFlush(inventory);

        Long id = inventory.getId();

        int rows = inventoryRepository.updateInventory(id, quantity);
        testEntityManager.clear();
        Inventory newInventory = testEntityManager.find(Inventory.class, id);

        assertEquals(quantityAfter, newInventory.getAvailableQuantity());
        assertEquals(0, rows);
    }



}
