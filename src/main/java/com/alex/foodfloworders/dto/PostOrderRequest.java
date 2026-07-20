package com.alex.foodfloworders.dto;

import com.alex.foodfloworders.model.Status;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PostOrderRequest(

        @NotNull
        Long foodId,

        @NotNull
        @Min(value = 0, message = "Quantity cannot be negative")
        @Max(value = 100, message = "Quantity cannot be more than 100")
        int quantity

) {}
