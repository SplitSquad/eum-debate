package com.example.debate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DebateApplication {

    public static void main(String[] args) {
        SpringApplication.run(DebateApplication.class, args);
    }

}
