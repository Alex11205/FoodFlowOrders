package com.alex.foodfloworders.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient inventoryRestClient(
            RestClient.Builder builder,
            @Value("${inventory.base-url}") String inventoryBaseUrl
    ) {
        return builder
                .baseUrl(inventoryBaseUrl)
                .build();
    }
}
