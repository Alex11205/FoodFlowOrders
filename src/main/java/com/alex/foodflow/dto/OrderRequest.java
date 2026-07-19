package com.alex.foodflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(

        @NotNull
        @Min(value = 0, message = "Quantity cannot be negative")
        @Max(value = 100, message = "Quantity cannot be more than 100")
        int quantity
) {}
