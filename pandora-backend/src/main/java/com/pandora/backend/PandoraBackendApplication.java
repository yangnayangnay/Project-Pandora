package com.pandora.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class PandoraBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PandoraBackendApplication.class, args);
        System.out.println("========================================");
        System.out.println("  Project: Pandora Backend Started!");
        System.out.println("  Smart Work System v1.0");
        System.out.println("  http://localhost:8080/api");
        System.out.println("========================================");
    }
}