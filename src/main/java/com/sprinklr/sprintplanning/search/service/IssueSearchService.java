package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import com.sprinklr.sprintplanning.search.dto.IssueSearchPageDto;

public interface IssueSearchService {

  IssueSearchPageDto searchInPod(String podId, IssueSearchFilters filters, int startAt, int maxResults);

  IssueSearchPageDto searchInRelease(
      String podId, String releaseId, IssueSearchFilters filters, int startAt, int maxResults);
}
