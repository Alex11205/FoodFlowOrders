package com.alex.foodflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inventory_foodName", columnNames = "foodName")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Food name cannot be empty")
    @Column(nullable = false)
    private String foodName;

    @NotEmpty
    private int availableQuantity;

}
