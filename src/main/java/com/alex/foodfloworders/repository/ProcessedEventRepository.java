package com.alex.foodfloworders.repository;


import com.alex.foodfloworders.model.Order;
import com.alex.foodfloworders.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventId(UUID eventID);


}
