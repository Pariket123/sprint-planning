package com.sprinklr.sprintplanning.team.service;

import com.sprinklr.sprintplanning.common.exception.ResourceNotFoundException;
import com.sprinklr.sprintplanning.team.dto.PodResponse;
import com.sprinklr.sprintplanning.team.dto.TeamResponse;
import com.sprinklr.sprintplanning.team.mapper.TeamMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.repository.PodRepository;
import com.sprinklr.sprintplanning.team.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamServiceImpl implements TeamService {

  private final TeamRepository teamRepository;
  private final PodRepository podRepository;
  private final TeamMapper teamMapper;

  public TeamServiceImpl(
      TeamRepository teamRepository,
      PodRepository podRepository,
      TeamMapper teamMapper) {
    this.teamRepository = teamRepository;
    this.podRepository = podRepository;
    this.teamMapper = teamMapper;
  }

  @Override
  public List<TeamResponse> getActiveTeams() {
    return teamMapper.toTeamResponses(teamRepository.findByActiveTrueOrderByNameAsc());
  }

  @Override
  public List<PodResponse> getActivePodsForTeam(String teamId) {
    ensureTeamExists(teamId);
    return teamMapper.toPodResponses(podRepository.findByTeamIdAndActiveTrueOrderByNameAsc(teamId));
  }

  @Override
  public PodResponse getActivePod(String podId) {
    return teamMapper.toPodResponse(getActivePodDocument(podId));
  }

  @Override
  public PodDocument getActivePodDocument(String podId) {
    return podRepository.findByIdAndActiveTrue(podId)
        .orElseThrow(() -> new ResourceNotFoundException("POD_NOT_FOUND", "Pod not found: " + podId));
  }

  private void ensureTeamExists(String teamId) {
    teamRepository.findById(teamId)
        .filter(team -> team.isActive())
        .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND", "Team not found: " + teamId));
  }
}
