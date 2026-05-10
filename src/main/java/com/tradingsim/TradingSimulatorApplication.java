package com.tradingsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Automated Trading Simulator application.
 */
@SpringBootApplication
public class TradingSimulatorApplication {

    /**
     * Starts the web application and initializes all Spring-managed components.
     */
    public static void main(String[] args) {
        SpringApplication.run(TradingSimulatorApplication.class, args);
    }
}
