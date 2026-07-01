package com.sprinklr.sprintplanning.planning.model;

public class CapacityAllocationPercents {

  private String key;
  private double roadmapPercent = 60.0;
  private double bugSupportPercent = 40.0;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public double getRoadmapPercent() {
    return roadmapPercent;
  }

  public void setRoadmapPercent(double roadmapPercent) {
    this.roadmapPercent = roadmapPercent;
  }

  public double getBugSupportPercent() {
    return bugSupportPercent;
  }

  public void setBugSupportPercent(double bugSupportPercent) {
    this.bugSupportPercent = bugSupportPercent;
  }
}
