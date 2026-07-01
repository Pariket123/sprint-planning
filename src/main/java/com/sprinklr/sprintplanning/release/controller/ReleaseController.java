package com.sprinklr.sprintplanning.release.controller;

import com.sprinklr.sprintplanning.common.dto.ApiResponse;
import com.sprinklr.sprintplanning.planning.dto.UpdateCapacityAllocationRequest;
import com.sprinklr.sprintplanning.release.dto.CreateReleaseRequest;
import com.sprinklr.sprintplanning.release.dto.ReleaseResponse;
import com.sprinklr.sprintplanning.release.dto.UpdateReleaseCapacityRequest;
import com.sprinklr.sprintplanning.release.dto.UpdateReleaseRequest;
import com.sprinklr.sprintplanning.release.service.ReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pods/{podId}/releases")
@Tag(name = "Releases", description = "Team-scoped release configuration (pod context required)")
public class ReleaseController {

  private final ReleaseService releaseService;

  public ReleaseController(ReleaseService releaseService) {
    this.releaseService = releaseService;
  }

  @GetMapping
  @Operation(summary = "List active releases for the pod's team")
  public ResponseEntity<ApiResponse<List<ReleaseResponse>>> listReleases(@PathVariable String podId) {
    return ResponseEntity.ok(ApiResponse.ok(releaseService.listReleases(podId)));
  }

  @GetMapping("/{releaseId}")
  @Operation(summary = "Get a team release (pod context required)")
  public ResponseEntity<ApiResponse<ReleaseResponse>> getRelease(
      @PathVariable String podId,
      @PathVariable String releaseId) {
    return ResponseEntity.ok(ApiResponse.ok(releaseService.getRelease(podId, releaseId)));
  }

  @PostMapping
  @Operation(summary = "Create a team release (pod context required)")
  public ResponseEntity<ApiResponse<ReleaseResponse>> createRelease(
      @PathVariable String podId,
      @Valid @RequestBody CreateReleaseRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(releaseService.createRelease(podId, request)));
  }

  @PutMapping("/{releaseId}")
  @Operation(summary = "Update a team release (pod context required)")
  public ResponseEntity<ApiResponse<ReleaseResponse>> updateRelease(
      @PathVariable String podId,
      @PathVariable String releaseId,
      @Valid @RequestBody UpdateReleaseRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(releaseService.updateRelease(podId, releaseId, request)));
  }

  @DeleteMapping("/{releaseId}")
  @Operation(summary = "Soft-delete/deactivate a team release (pod context required)")
  public ResponseEntity<ApiResponse<ReleaseResponse>> deactivateRelease(
      @PathVariable String podId,
      @PathVariable String releaseId) {
    return ResponseEntity.ok(ApiResponse.ok(releaseService.deactivateRelease(podId, releaseId)));
  }

  @PutMapping("/{releaseId}/capacity")
  @Operation(summary = "Upsert team capacity inputs for a release")
  public ResponseEntity<ApiResponse<ReleaseResponse>> updateCapacity(
      @PathVariable String podId,
      @PathVariable String releaseId,
      @Valid @RequestBody UpdateReleaseCapacityRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(releaseService.updateCapacity(podId, releaseId, request)));
  }

  @PutMapping("/{releaseId}/capacity-allocation")
  @Operation(summary = "Upsert roadmap vs bug/support capacity split percentages for a release")
  public ResponseEntity<ApiResponse<ReleaseResponse>> updateCapacityAllocation(
      @PathVariable String podId,
      @PathVariable String releaseId,
      @Valid @RequestBody UpdateCapacityAllocationRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(
        releaseService.updateCapacityAllocation(podId, releaseId, request.capacityAllocation())));
  }
}
