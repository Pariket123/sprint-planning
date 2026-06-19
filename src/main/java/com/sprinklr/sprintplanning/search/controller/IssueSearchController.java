package com.sprinklr.sprintplanning.search.controller;

import com.sprinklr.sprintplanning.common.dto.ApiResponse;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import com.sprinklr.sprintplanning.search.dto.IssueSearchPageDto;
import com.sprinklr.sprintplanning.search.service.IssueSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Issue Search", description = "Dynamic Jira issue search with filter-based drill-down")
public class IssueSearchController {

  private final IssueSearchService issueSearchService;

  public IssueSearchController(IssueSearchService issueSearchService) {
    this.issueSearchService = issueSearchService;
  }

  @PostMapping("/api/v1/pods/{podId}/issues/search")
  @Operation(summary = "Search issues in a pod using dynamic filters")
  public ResponseEntity<ApiResponse<IssueSearchPageDto>> searchInPod(
      @PathVariable String podId,
      @RequestParam(defaultValue = "0") int startAt,
      @RequestParam(defaultValue = "50") int maxResults,
      @RequestBody(required = false) IssueSearchFilters filters) {
    return ResponseEntity.ok(ApiResponse.ok(
        issueSearchService.searchInPod(podId, filters, startAt, maxResults)));
  }

  @PostMapping("/api/v1/pods/{podId}/releases/{releaseId}/issues/search")
  @Operation(summary = "Search issues in a pod release using release config merged with request filters")
  public ResponseEntity<ApiResponse<IssueSearchPageDto>> searchInRelease(
      @PathVariable String podId,
      @PathVariable String releaseId,
      @RequestParam(defaultValue = "0") int startAt,
      @RequestParam(defaultValue = "50") int maxResults,
      @RequestBody(required = false) IssueSearchFilters filters) {
    return ResponseEntity.ok(ApiResponse.ok(
        issueSearchService.searchInRelease(podId, releaseId, filters, startAt, maxResults)));
  }
}
