package com.sprinklr.sprintplanning.planning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class PlanningAsyncConfig {

  @Bean(destroyMethod = "close")
  Executor jiraFetchExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
