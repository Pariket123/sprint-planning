package com.sprinklr.sprintplanning.planning.controller;

import com.sprinklr.sprintplanning.common.dto.ApiResponse;
import com.sprinklr.sprintplanning.planning.dto.RecordRolloverRequest;
import com.sprinklr.sprintplanning.planning.dto.RolloverIssueDto;
import com.sprinklr.sprintplanning.planning.service.RolloverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pods/{podId}/sprints")
@Tag(name = "Sprint Rollover", description = "Rollover history tracking between sprints")
public class RolloverController {

  private final RolloverService rolloverService;

  public RolloverController(RolloverService rolloverService) {
    this.rolloverService = rolloverService;
  }

  @PostMapping("/{fromSprintId}/rollover")
  @Operation(summary = "Record rollover from one sprint to another")
  public ResponseEntity<ApiResponse<RolloverIssueDto>> recordRollover(
      @PathVariable String podId,
      @PathVariable Long fromSprintId,
      @Valid @RequestBody RecordRolloverRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(
        rolloverService.recordRollover(podId, fromSprintId, request)));
  }

  @GetMapping("/{jiraSprintId}/rollover")
  @Operation(summary = "Get all rollover records involving a sprint")
  public ResponseEntity<ApiResponse<List<RolloverIssueDto>>> getRolloverRecords(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId) {
    return ResponseEntity.ok(ApiResponse.ok(rolloverService.getRolloverRecords(podId, jiraSprintId)));
  }

  @GetMapping("/{jiraSprintId}/rollover/outgoing")
  @Operation(summary = "Get outgoing rollover records from a sprint")
  public ResponseEntity<ApiResponse<List<RolloverIssueDto>>> getOutgoingRollovers(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId) {
    return ResponseEntity.ok(ApiResponse.ok(rolloverService.getOutgoingRollovers(podId, jiraSprintId)));
  }

  @GetMapping("/{jiraSprintId}/rollover/incoming")
  @Operation(summary = "Get incoming rollover records into a sprint")
  public ResponseEntity<ApiResponse<List<RolloverIssueDto>>> getIncomingRollovers(
      @PathVariable String podId,
      @PathVariable Long jiraSprintId) {
    return ResponseEntity.ok(ApiResponse.ok(rolloverService.getIncomingRollovers(podId, jiraSprintId)));
  }
}
