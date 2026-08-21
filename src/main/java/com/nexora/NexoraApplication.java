package com.nexora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Nexora backend.
 *
 * WHAT THIS IS:
 * Spring Boot needs one class annotated with @SpringBootApplication.
 * When you run this class, Spring Boot:
 *   1. Starts an embedded web server (Tomcat) on port 8080 (by default)
 *   2. Scans the com.nexora package (and sub-packages) for components
 *      (controllers, services, repositories) and wires them together
 *   3. Connects to MySQL using the settings in application.properties
 *
 * You do not need to write any server code yourself — Spring Boot does it.
 */
@SpringBootApplication
public class NexoraApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexoraApplication.class, args);
    }

}
