package com.alex.foodflow.repository;

import com.alex.foodflow.dto.UpdateInventoryResponse;
import com.alex.foodflow.model.Inventory;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {


    @Transactional
    @Modifying
    @Query(
            value = """
                    UPDATE 
                        inventory
                    SET
                        available_quantity = available_quantity - :quantity
                    WHERE
                        id = :id
                        AND available_quantity >= :quantity;
                    """
            , nativeQuery = true)
    void updateInventory(@Param("id") Long id, @Param("quantity") int quantity);
}
