package com.example.cdaxVideo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CdaxVideoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdaxVideoApplication.class, args);
    }
}