package com.pyrite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Pyrite server.
 * <p>
 * {@link SpringBootApplication} is a convenience annotation that turns this class into a Spring Boot app:
 * it enables component scanning (finds {@code @Service}, {@code @RestController}, etc.),
 * auto-configuration (embedded web server, JSON, validation), and treats this class as the configuration source.
 * <p>
 * Running {@code main} starts an embedded web server (Tomcat by default) so HTTP requests can reach our REST API.
 */
@SpringBootApplication
public class PyriteApplication {

    public static void main(String[] args) {
        SpringApplication.run(PyriteApplication.class, args);
    }
}
