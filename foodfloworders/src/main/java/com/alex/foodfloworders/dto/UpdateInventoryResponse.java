package com.alex.foodfloworders.dto;

public record UpdateInventoryResponse(

        String foodName,
        int availableQuantity
) {}
