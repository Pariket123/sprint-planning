package com.sprinklr.sprintplanning.planning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planning")
public class PlanningProperties {

  private double domainImbalanceThreshold = 0.6;
  private double highUtilizationThreshold = 0.9;
  private double mediumUtilizationThreshold = 0.75;

  public double getDomainImbalanceThreshold() {
    return domainImbalanceThreshold;
  }

  public void setDomainImbalanceThreshold(double domainImbalanceThreshold) {
    this.domainImbalanceThreshold = domainImbalanceThreshold;
  }

  public double getHighUtilizationThreshold() {
    return highUtilizationThreshold;
  }

  public void setHighUtilizationThreshold(double highUtilizationThreshold) {
    this.highUtilizationThreshold = highUtilizationThreshold;
  }

  public double getMediumUtilizationThreshold() {
    return mediumUtilizationThreshold;
  }

  public void setMediumUtilizationThreshold(double mediumUtilizationThreshold) {
    this.mediumUtilizationThreshold = mediumUtilizationThreshold;
  }
}
