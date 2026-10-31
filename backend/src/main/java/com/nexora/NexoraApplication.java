package com.nexora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the Nexora backend.
 *
 * Configured with @EnableScheduling for SSE heartbeat broadcasts,
 * cold chain expiry monitoring, and batch dispatch routines.
 */
@SpringBootApplication
@EnableScheduling
public class NexoraApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexoraApplication.class, args);
    }

}
