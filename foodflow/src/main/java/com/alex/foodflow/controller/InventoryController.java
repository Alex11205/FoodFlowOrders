package com.alex.foodflow.controller;

import com.alex.foodflow.dto.OrderRequest;
import com.alex.foodflow.dto.UpdateInventoryResponse;
import com.alex.foodflow.service.InventoryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/inventory")
@AllArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

//    @PostMapping("/{id}/reserve")
//    public ResponseEntity<Integer> updateInventory(@Valid @RequestBody OrderRequest orderRequest,
//                                                                   @PathVariable Long id) {
//
//        int quantity = orderRequest.quantity();
//        int rows = inventoryService.updateInventory(id, quantity);
//
//        return ResponseEntity.ok()
//                .body(rows);
//
//    }
}
