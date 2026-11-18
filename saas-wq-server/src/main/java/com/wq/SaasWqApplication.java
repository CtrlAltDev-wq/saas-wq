package com.wq;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * saas-wq backend entry point.
 *
 * <p>Component scanning starts from {@code com.wq} so that the common, framework and
 * server modules are all picked up from a single boot application.</p>
 */
@SpringBootApplication
@MapperScan("com.wq.server.mapper")
public class SaasWqApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SaasWqApplication.class, args);
    }
}
