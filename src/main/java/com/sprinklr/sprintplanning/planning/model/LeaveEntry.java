package com.sprinklr.sprintplanning.planning.model;

import com.sprinklr.sprintplanning.common.enums.Domain;

import java.time.LocalDate;

public class LeaveEntry {

  private LocalDate startDate;
  private LocalDate endDate;
  private Domain domain;
  private LeaveType type = LeaveType.LEAVE;

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public LeaveType getType() {
    return type;
  }

  public void setType(LeaveType type) {
    this.type = type;
  }
}
