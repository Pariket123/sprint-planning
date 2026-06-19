package com.sprinklr.sprintplanning.planning.model;

import com.sprinklr.sprintplanning.common.enums.Domain;

public class DomainCapacity {

  private Domain domain;
  private int headcount;
  private double bandwidthPercent;
  private Double manualAdjustment;

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public int getHeadcount() {
    return headcount;
  }

  public void setHeadcount(int headcount) {
    this.headcount = headcount;
  }

  public double getBandwidthPercent() {
    return bandwidthPercent;
  }

  public void setBandwidthPercent(double bandwidthPercent) {
    this.bandwidthPercent = bandwidthPercent;
  }

  public Double getManualAdjustment() {
    return manualAdjustment;
  }

  public void setManualAdjustment(Double manualAdjustment) {
    this.manualAdjustment = manualAdjustment;
  }
}
