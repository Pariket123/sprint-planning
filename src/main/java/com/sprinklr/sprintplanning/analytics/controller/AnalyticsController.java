package com.sprinklr.sprintplanning.analytics.controller;

import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.analytics.service.AnalyticsService;
import com.sprinklr.sprintplanning.common.dto.ApiResponse;
import com.sprinklr.sprintplanning.common.model.SprintView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pods/{podId}")
@Tag(name = "Analytics", description = "Read-only sprint analytics from live Jira data")
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  public AnalyticsController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @GetMapping("/sprints")
  @Operation(summary = "List sprints for a pod from Jira")
  public ResponseEntity<ApiResponse<List<SprintView>>> listSprints(
      @PathVariable String podId,
      @RequestParam(defaultValue = "active,future,closed") String state) {
    return ResponseEntity.ok(ApiResponse.ok(analyticsService.getSprints(podId, state)));
  }

  @GetMapping("/sprints/{jiraSprintId}/analytics")
  @Operation(summary = "Get sprint analytics insights")
  public ResponseEntity<ApiResponse<AnalyticsResponse>> getSprintAnalytics(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId,
      @RequestParam(required = false) String issueTypeProfile) {
    return ResponseEntity.ok(
        ApiResponse.ok(analyticsService.getSprintAnalytics(podId, jiraSprintId, issueTypeProfile)));
  }
}
