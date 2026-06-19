package com.sprinklr.sprintplanning.planning.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PlanningProperties.class)
public class PlanningConfig {
}
