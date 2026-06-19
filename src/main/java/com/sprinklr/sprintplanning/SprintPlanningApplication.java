package com.sprinklr.sprintplanning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class SprintPlanningApplication {

    public static void main(String[] args) {
        SpringApplication.run(SprintPlanningApplication.class, args);
    }
}
