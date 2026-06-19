package com.sprinklr.sprintplanning.client.jira.config;

import com.sprinklr.sprintplanning.common.exception.JiraRetryableException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
public class JiraRetryConfig {

  @Bean
  public RetryTemplate jiraRetryTemplate(JiraProperties properties) {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
        properties.getRetry().getMaxAttempts(),
        Map.of(JiraRetryableException.class, true));

    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(properties.getRetry().getInitialIntervalMs());
    backOffPolicy.setMultiplier(properties.getRetry().getMultiplier());

    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    return retryTemplate;
  }
}
