package com.example.demo.config;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.context.annotation.Configuration;

/**
 * Mongock configuration to enable MongoDB migrations
 */
@Configuration
@EnableMongock
public class MongockConfig {
    // Mongock will automatically scan for @ChangeUnit classes
    // in the package specified in application.yml
}
