package com.sprinklr.sprintplanning.planning.controller;

import com.sprinklr.sprintplanning.common.dto.ApiResponse;
import com.sprinklr.sprintplanning.planning.dto.PlanningDataDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningValidationResultDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningViewDto;
import com.sprinklr.sprintplanning.planning.dto.UpdateCapacityRequest;
import com.sprinklr.sprintplanning.planning.dto.UpdateLeavesRequest;
import com.sprinklr.sprintplanning.planning.dto.UpdateOverridesRequest;
import com.sprinklr.sprintplanning.planning.mapper.PlanningMapper;
import com.sprinklr.sprintplanning.planning.service.PlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pods/{podId}/sprints/{jiraSprintId}/planning")
@Tag(name = "Sprint Planning", description = "Interactive sprint planning with capacity and validation")
public class PlanningController {

  private final PlanningService planningService;
  private final PlanningMapper planningMapper;

  public PlanningController(PlanningService planningService, PlanningMapper planningMapper) {
    this.planningService = planningService;
    this.planningMapper = planningMapper;
  }

  @GetMapping
  @Operation(summary = "Get merged planning view with Jira sprint issues and persisted inputs")
  public ResponseEntity<ApiResponse<PlanningViewDto>> getPlanning(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId) {
    return ResponseEntity.ok(ApiResponse.ok(planningService.getPlanningView(podId, jiraSprintId)));
  }

  @PutMapping("/capacity")
  @Operation(summary = "Upsert per-domain capacity inputs")
  public ResponseEntity<ApiResponse<PlanningDataDto>> updateCapacity(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId,
      @Valid @RequestBody UpdateCapacityRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(
        planningMapper.toPlanningDataDto(
            planningService.updateCapacity(podId, jiraSprintId, request.capacity()))));
  }

  @PutMapping("/leaves")
  @Operation(summary = "Upsert leaves and holidays")
  public ResponseEntity<ApiResponse<PlanningDataDto>> updateLeaves(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId,
      @Valid @RequestBody UpdateLeavesRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(
        planningMapper.toPlanningDataDto(
            planningService.updateLeaves(podId, jiraSprintId, request.leaves()))));
  }

  @PutMapping("/overrides")
  @Operation(summary = "Upsert planning overrides for backlog/sprint issue selection")
  public ResponseEntity<ApiResponse<PlanningDataDto>> updateOverrides(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId,
      @Valid @RequestBody UpdateOverridesRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(
        planningMapper.toPlanningDataDto(
            planningService.updateOverrides(podId, jiraSprintId, request.overrides()))));
  }

  @GetMapping("/summary")
  @Operation(summary = "Get computed planning summary")
  public ResponseEntity<ApiResponse<PlanningSummaryDto>> getSummary(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId) {
    return ResponseEntity.ok(ApiResponse.ok(planningService.calculateSummary(podId, jiraSprintId)));
  }

  @PostMapping("/validate")
  @Operation(summary = "Validate planning and return warnings")
  public ResponseEntity<ApiResponse<PlanningValidationResultDto>> validate(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId) {
    return ResponseEntity.ok(ApiResponse.ok(planningService.validate(podId, jiraSprintId)));
  }
}
