package com.alex.foodflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableResilientMethods
public class FoodflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodflowApplication.class, args);
    }

}
