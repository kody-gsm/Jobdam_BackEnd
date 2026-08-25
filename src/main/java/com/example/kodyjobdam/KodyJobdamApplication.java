package com.example.kodyjobdam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KodyJobdamApplication {

    public static void main(String[] args) {
        SpringApplication.run(KodyJobdamApplication.class, args);
    }

}
