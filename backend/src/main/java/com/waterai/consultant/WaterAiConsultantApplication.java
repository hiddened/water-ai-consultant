package com.waterai.consultant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class WaterAiConsultantApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaterAiConsultantApplication.class, args);
    }
}
