package com.alex.foodflow.repository;

import com.alex.foodflow.dto.UpdateInventoryResponse;
import com.alex.foodflow.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {


//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Retryable(includes = {OptimisticLockingFailureException.class}, maxRetries = 3)
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
                        AND available_quantity >= :quantity
                    """
            , nativeQuery = true)
    int updateInventory(@Param("id") Long id, @Param("quantity") int quantity);


}
