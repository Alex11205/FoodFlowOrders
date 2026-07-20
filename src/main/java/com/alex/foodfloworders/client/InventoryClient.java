package com.alex.foodfloworders.client;

import com.alex.foodfloworders.dto.InventoryReservationRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class InventoryClient {

    private final RestClient inventoryRestClient;

    public InventoryClient(@Qualifier("inventoryRestClient") RestClient inventoryRestClient) {
        this.inventoryRestClient = inventoryRestClient;
    }

    public boolean reserveInventory(Long foodId, int quantity) {
        try {
            Integer updatedRows = inventoryRestClient.post()
                    .uri("/inventory/{id}/reserve", foodId)
                    .body(new InventoryReservationRequest(quantity))
                    .retrieve()
                    .body(Integer.class);

            return updatedRows != null && updatedRows > 0;
        } catch (HttpClientErrorException.Conflict ex) {
            return false;
        }
    }
}
