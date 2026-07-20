package com.alex.foodfloworders.integration;

import com.alex.foodflow.exceptions.InsufficientInventoryException;
import com.alex.foodflow.model.Inventory;
import com.alex.foodflow.repository.InventoryRepository;
import com.alex.foodflow.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Testcontainers
@Sql(
        statements = {
                "TRUNCATE TABLE inventory RESTART IDENTITY CASCADE"
        },
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
public class OrderConcurrencyIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");
    static {
        postgres.start();
    }

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void onlyAsManyConcurrentReservationsAsAvailableStockShouldSucceed() throws InterruptedException {
        int availableQuantity = 10;
        int requestNumber = 100;

        Inventory saved = inventoryRepository.save(new Inventory("foodName", availableQuantity));
        Long id = saved.getId();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startingGate = new CountDownLatch(1);

        List<Callable<Boolean>> tasks = IntStream.range(0, requestNumber)
                .<Callable<Boolean>>mapToObj(i -> () -> {
                    startingGate.await();
                    try {
                        inventoryService.updateInventory(id, 1);
                        return true;
                    } catch (InsufficientInventoryException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        List<Future<Boolean>> futures = tasks.stream()
                .map(executor::submit)
                .collect(Collectors.toList());

        startingGate.countDown();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        for (Future<Boolean> future : futures) {
            try {
                boolean succeeded = future.get(30, TimeUnit.SECONDS);
                (succeeded ? successCount : failureCount).incrementAndGet();
            } catch (Exception e) {
                fail("Reservation attempt threw an unexpected exception: " + e.getCause());
            }
        }
        executor.shutdown();

        assertEquals(availableQuantity, successCount.get());
        assertEquals(requestNumber - availableQuantity, failureCount.get());

        Inventory finalState = inventoryRepository.findById(id).orElseThrow();
        assertEquals(0, finalState.getAvailableQuantity());
    }
}
