package com.sprinklr.sprintplanning.planning.controller;

import com.sprinklr.sprintplanning.TestSecurityConfig;
import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.planning.dto.BacklogPageDto;
import com.sprinklr.sprintplanning.planning.dto.IssueMoveRequest;
import com.sprinklr.sprintplanning.planning.dto.PlanningViewDto;
import com.sprinklr.sprintplanning.planning.service.PlanningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlanningOperationsController.class)
@Import({TestSecurityConfig.class, com.sprinklr.sprintplanning.common.handler.GlobalExceptionHandler.class})
class PlanningOperationsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PlanningService planningService;

  @Test
  void getBacklogReturnsEnvelope() throws Exception {
    when(planningService.getBacklog("pod-1", 0, 50))
        .thenReturn(new BacklogPageDto(List.of(), 0, 50, 0, true));

    mockMvc.perform(get("/api/v1/pods/pod-1/backlog"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.last").value(true));
  }

  @Test
  void moveIssuesToSprintReturnsRefreshedPlanningView() throws Exception {
    PlanningViewDto view = new PlanningViewDto(
        10L,
        new SprintView(10L, "Sprint 10", "active", Instant.now(), Instant.now(), null),
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        Map.of(),
        0,
        0,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
    when(planningService.moveIssuesToSprint(eq("pod-1"), eq(10L), any(IssueMoveRequest.class)))
        .thenReturn(view);

    mockMvc.perform(post("/api/v1/pods/pod-1/sprints/10/issues/move")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "issueKeys": ["WFM-1", "WFM-2"] }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.jiraSprintId").value(10));

    verify(planningService).moveIssuesToSprint(eq("pod-1"), eq(10L), any(IssueMoveRequest.class));
  }

  @Test
  void moveIssuesToBacklogReturnsRefreshedBacklogPage() throws Exception {
    when(planningService.moveIssuesToBacklog("pod-1", 0, 50, List.of("WFM-3")))
        .thenReturn(new BacklogPageDto(List.of(), 0, 50, 1, true));

    mockMvc.perform(post("/api/v1/pods/pod-1/backlog/move")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "issueKeys": ["WFM-3"] }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.total").value(1));
  }
}
