package com.alex.foodflow.dto;

public record UpdateInventoryResponse(

        String foodName,
        int availableQuantity
) {}
