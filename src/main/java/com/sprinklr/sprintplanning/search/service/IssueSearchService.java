package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import com.sprinklr.sprintplanning.search.dto.IssueSearchPageDto;
import com.sprinklr.sprintplanning.search.dto.IssueSearchReleaseRequest;

public interface IssueSearchService {

  IssueSearchPageDto searchInPod(String podId, IssueSearchFilters filters, int startAt, int maxResults);

  IssueSearchPageDto searchInRelease(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request,
      int startAt,
      int maxResults);

  AnalyticsResponse analyzeRelease(
      String podId,
      String releaseId,
      IssueSearchReleaseRequest request);
}
