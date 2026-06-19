package com.sprinklr.sprintplanning.team.service;

import com.sprinklr.sprintplanning.team.dto.PodResponse;
import com.sprinklr.sprintplanning.team.dto.TeamResponse;
import com.sprinklr.sprintplanning.team.model.PodDocument;

import java.util.List;

public interface TeamService {

  List<TeamResponse> getActiveTeams();

  List<PodResponse> getActivePodsForTeam(String teamId);

  PodResponse getActivePod(String podId);

  PodDocument getActivePodDocument(String podId);
}
