package com.sprinklr.sprintplanning.team.controller;

import com.sprinklr.sprintplanning.TestSecurityConfig;
import com.sprinklr.sprintplanning.common.exception.ResourceNotFoundException;
import com.sprinklr.sprintplanning.team.dto.PodJiraConfigSummary;
import com.sprinklr.sprintplanning.team.dto.PodResponse;
import com.sprinklr.sprintplanning.team.dto.TeamResponse;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TeamController.class)
@Import({TestSecurityConfig.class, com.sprinklr.sprintplanning.common.handler.GlobalExceptionHandler.class})
class TeamControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TeamService teamService;

  @Test
  void listTeamsReturnsEnvelope() throws Exception {
    when(teamService.getActiveTeams()).thenReturn(List.of(
        new TeamResponse("team-1", "WFM", "Workforce Management", true)));

    mockMvc.perform(get("/api/v1/teams"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].code").value("WFM"));
  }

  @Test
  void getPodReturnsEnvelope() throws Exception {
    PodResponse pod = new PodResponse(
        "pod-1",
        "team-1",
        "FORECASTING",
        "Forecasting",
        true,
        new PodJiraConfigSummary(101L, List.of("WFM"), "customfield_10016", "customfield_10020",
            Map.of("DEV", "Dev")));
    when(teamService.getActivePod("pod-1")).thenReturn(pod);

    mockMvc.perform(get("/api/v1/pods/pod-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.code").value("FORECASTING"));
  }

  @Test
  void notFoundReturnsErrorEnvelope() throws Exception {
    when(teamService.getActivePod("missing"))
        .thenThrow(new ResourceNotFoundException("POD_NOT_FOUND", "Pod not found: missing"));

    mockMvc.perform(get("/api/v1/pods/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("POD_NOT_FOUND"));
  }
}
