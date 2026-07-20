package com.alex.foodflow.service;

import com.alex.foodflow.dto.UpdateInventoryResponse;
import com.alex.foodflow.exceptions.InsufficientInventoryException;
import com.alex.foodflow.repository.InventoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;


    public int updateInventory(Long id, int quantity) {

        int updatedInventory = inventoryRepository.updateInventory(id, quantity);

        if (updatedInventory == 0)
            throw new InsufficientInventoryException("Insufficient inventory");

        return updatedInventory;
    }
}
