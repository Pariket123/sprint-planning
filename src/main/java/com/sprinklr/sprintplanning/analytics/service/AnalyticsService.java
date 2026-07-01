package com.sprinklr.sprintplanning.analytics.service;

import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.common.model.SprintView;

import java.util.List;

public interface AnalyticsService {

  List<SprintView> getSprints(String podId, String state);

  AnalyticsResponse getSprintAnalytics(String podId, Long jiraSprintId, String issueTypeProfile);
}
