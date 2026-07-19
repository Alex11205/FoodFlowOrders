package com.alex.foodflow.service;

import com.alex.foodflow.dto.UpdateInventoryResponse;
import com.alex.foodflow.repository.InventoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public void updateInventory(Long id, int quantity) {

        inventoryRepository.updateInventory(id, quantity);

    }
}
