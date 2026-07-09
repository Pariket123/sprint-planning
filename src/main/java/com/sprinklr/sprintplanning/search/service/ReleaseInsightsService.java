package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.release.dto.ReleaseCapacitySummaryDto;
import com.sprinklr.sprintplanning.search.dto.IssueSearchReleaseRequest;

public interface ReleaseInsightsService {

  AnalyticsResponse analyzeRelease(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request);

  ReleaseCapacitySummaryDto calculateReleaseCapacityMetrics(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request);
}
