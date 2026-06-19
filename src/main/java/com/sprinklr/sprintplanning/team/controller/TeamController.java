package com.sprinklr.sprintplanning.team.controller;

import com.sprinklr.sprintplanning.common.dto.ApiResponse;
import com.sprinklr.sprintplanning.team.dto.PodResponse;
import com.sprinklr.sprintplanning.team.dto.TeamResponse;
import com.sprinklr.sprintplanning.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Teams", description = "Team and pod navigation")
public class TeamController {

  private final TeamService teamService;

  public TeamController(TeamService teamService) {
    this.teamService = teamService;
  }

  @GetMapping("/teams")
  @Operation(summary = "List active teams")
  public ResponseEntity<ApiResponse<List<TeamResponse>>> listTeams() {
    return ResponseEntity.ok(ApiResponse.ok(teamService.getActiveTeams()));
  }

  @GetMapping("/teams/{teamId}/pods")
  @Operation(summary = "List active pods for a team")
  public ResponseEntity<ApiResponse<List<PodResponse>>> listPods(@PathVariable String teamId) {
    return ResponseEntity.ok(ApiResponse.ok(teamService.getActivePodsForTeam(teamId)));
  }

  @GetMapping("/pods/{podId}")
  @Operation(summary = "Get pod details including Jira configuration summary")
  public ResponseEntity<ApiResponse<PodResponse>> getPod(@PathVariable String podId) {
    return ResponseEntity.ok(ApiResponse.ok(teamService.getActivePod(podId)));
  }
}
