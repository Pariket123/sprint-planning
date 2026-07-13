package com.sprinklr.sprintplanning.planning.calculator;

import java.util.LinkedHashSet;
import java.util.Set;

final class SelectionMetrics {

  static final SelectionMetrics EMPTY = new SelectionMetrics();

  private double storyPoints;
  private int issueCount;
  private final Set<String> countedIssueKeys = new LinkedHashSet<>();

  void addStoryPoints(double points) {
    storyPoints += points;
  }

  void recordUniqueIssue(String issueKey) {
    if (issueKey == null || issueKey.isBlank()) {
      issueCount++;
      return;
    }
    if (countedIssueKeys.add(issueKey)) {
      issueCount++;
    }
  }

  double storyPoints() {
    return storyPoints;
  }

  int issueCount() {
    return issueCount;
  }
}
