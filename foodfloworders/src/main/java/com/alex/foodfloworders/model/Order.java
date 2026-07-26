package com.alex.foodfloworders.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "foodId cannot be empty")
    @Column(nullable = false)
    private Long foodId;

    @NotNull(message = "Quantity cannot be empty")
    @Column(nullable = false)
    private int quantity;

    @NotNull(message = "Status cannot be empty")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "order_status")
    private Status status;

    @NotNull(message = "createdAt cannot be empty")
    @Column(nullable = false)
    private Instant createdAt;


}
