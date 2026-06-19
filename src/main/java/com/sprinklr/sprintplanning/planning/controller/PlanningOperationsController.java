package com.sprinklr.sprintplanning.planning.controller;

import com.sprinklr.sprintplanning.common.dto.ApiResponse;
import com.sprinklr.sprintplanning.planning.dto.BacklogPageDto;
import com.sprinklr.sprintplanning.planning.dto.IssueMoveRequest;
import com.sprinklr.sprintplanning.planning.dto.PlanningViewDto;
import com.sprinklr.sprintplanning.planning.service.PlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pods/{podId}")
@Tag(name = "Sprint Planning", description = "Sprint operations and backlog management")
public class PlanningOperationsController {

  private final PlanningService planningService;

  public PlanningOperationsController(PlanningService planningService) {
    this.planningService = planningService;
  }

  @GetMapping("/backlog")
  @Operation(summary = "Get live backlog issues from Jira (paginated)")
  public ResponseEntity<ApiResponse<BacklogPageDto>> getBacklog(
      @PathVariable String podId,
      @RequestParam(defaultValue = "0") int startAt,
      @RequestParam(defaultValue = "50") int maxResults) {
    return ResponseEntity.ok(ApiResponse.ok(planningService.getBacklog(podId, startAt, maxResults)));
  }

  @PostMapping("/backlog/move")
  @Operation(summary = "Bulk move issues to backlog")
  public ResponseEntity<ApiResponse<BacklogPageDto>> moveIssuesToBacklog(
      @PathVariable String podId,
      @RequestParam(defaultValue = "0") int startAt,
      @RequestParam(defaultValue = "50") int maxResults,
      @Valid @RequestBody IssueMoveRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(
        planningService.moveIssuesToBacklog(podId, startAt, maxResults, request.issueKeys())));
  }

  @PostMapping("/sprints/{jiraSprintId}/issues/move")
  @Operation(summary = "Bulk move issues into a sprint")
  public ResponseEntity<ApiResponse<PlanningViewDto>> moveIssuesToSprint(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId,
      @Valid @RequestBody IssueMoveRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(
        planningService.moveIssuesToSprint(podId, jiraSprintId, request)));
  }
}
