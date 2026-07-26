package com.alex.foodflow.repository;

import com.alex.foodflow.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventId(UUID eventId);

    @Modifying
    @Query(value = """
            INSERT INTO processed_event(event_id, consumer_name, processed_at)
            VALUES (:eventId, :consumerName, CURRENT_TIMESTAMP)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int claim(
            @Param("eventId") UUID eventId,
            @Param("consumerName") String consumerName
    );
}
