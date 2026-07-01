package com.sprinklr.sprintplanning.planning.model;

import com.sprinklr.sprintplanning.common.enums.Domain;

public class PersonCapacity {

  private String personName;
  private Domain domain;
  private double bandwidthPercent;

  private double velocity = 1.0;

  /** Legacy domain-wise rows stored in Mongo before person-wise capacity. */
  private Integer headcount;

  public String getPersonName() {
    return personName;
  }

  public void setPersonName(String personName) {
    this.personName = personName;
  }

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public double getBandwidthPercent() {
    return bandwidthPercent;
  }

  public void setBandwidthPercent(double bandwidthPercent) {
    this.bandwidthPercent = bandwidthPercent;
  }

  public double getVelocity() {
    return velocity > 0 ? velocity : 1.0;
  }

  public void setVelocity(double velocity) {
    this.velocity = velocity;
  }

  public Integer getHeadcount() {
    return headcount;
  }

  public void setHeadcount(Integer headcount) {
    this.headcount = headcount;
  }
}
