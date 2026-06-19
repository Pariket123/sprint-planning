package com.sprinklr.sprintplanning.client.jira.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jira")
public class JiraProperties {

  private String baseUrl;
  private String email;
  private String apiToken;
  private Retry retry = new Retry();

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getApiToken() {
    return apiToken;
  }

  public void setApiToken(String apiToken) {
    this.apiToken = apiToken;
  }

  public Retry getRetry() {
    return retry;
  }

  public void setRetry(Retry retry) {
    this.retry = retry;
  }

  public static class Retry {

    private int maxAttempts = 3;
    private long initialIntervalMs = 500;
    private double multiplier = 2.0;

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public long getInitialIntervalMs() {
      return initialIntervalMs;
    }

    public void setInitialIntervalMs(long initialIntervalMs) {
      this.initialIntervalMs = initialIntervalMs;
    }

    public double getMultiplier() {
      return multiplier;
    }

    public void setMultiplier(double multiplier) {
      this.multiplier = multiplier;
    }
  }
}
