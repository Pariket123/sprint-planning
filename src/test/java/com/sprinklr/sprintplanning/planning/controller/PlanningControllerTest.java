package com.sprinklr.sprintplanning.planning.controller;

import com.sprinklr.sprintplanning.TestSecurityConfig;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.planning.dto.DomainPlanningMetricsDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningDataDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningValidationResultDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningViewDto;
import com.sprinklr.sprintplanning.planning.dto.RiskLevel;
import com.sprinklr.sprintplanning.planning.mapper.PlanningMapper;
import com.sprinklr.sprintplanning.planning.model.DomainCapacity;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlanningController.class)
@Import({TestSecurityConfig.class, com.sprinklr.sprintplanning.common.handler.GlobalExceptionHandler.class})
class PlanningControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PlanningService planningService;

  @MockBean
  private PlanningMapper planningMapper;

  @Test
  void getPlanningReturnsEnvelope() throws Exception {
    PlanningViewDto view = new PlanningViewDto(
        10L,
        new SprintView(10L, "Sprint 10", "active", Instant.now(), Instant.now(), null),
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        Map.of("DEV", 2.0),
        List.of(),
        List.of());
    when(planningService.getPlanningView("pod-1", 10L)).thenReturn(view);

    mockMvc.perform(get("/api/v1/pods/pod-1/sprints/10/planning"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.jiraSprintId").value(10))
        .andExpect(jsonPath("$.data.resolvedRollover.DEV").value(2.0));
  }

  @Test
  void updateCapacityReturnsEnvelope() throws Exception {
    SprintPlanningDocument document = new SprintPlanningDocument();
    document.setId("plan-1");
    document.setPodId("pod-1");
    document.setJiraSprintId(10L);
    DomainCapacity capacity = new DomainCapacity();
    capacity.setDomain(Domain.DEV);
    capacity.setHeadcount(2);
    capacity.setBandwidthPercent(100);
    document.setCapacity(List.of(capacity));

    PlanningDataDto dataDto = new PlanningDataDto(
        "plan-1", "pod-1", 10L, List.of(capacity), List.of(), List.of(), Map.of(), Instant.now());

    when(planningService.updateCapacity(eq("pod-1"), eq(10L), any())).thenReturn(document);
    when(planningMapper.toPlanningDataDto(document)).thenReturn(dataDto);

    mockMvc.perform(put("/api/v1/pods/pod-1/sprints/10/planning/capacity")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "capacity": [
                    {
                      "domain": "DEV",
                      "headcount": 2,
                      "bandwidthPercent": 100
                    }
                  ]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.capacity[0].headcount").value(2));
  }

  @Test
  void getSummaryReturnsEnvelope() throws Exception {
    PlanningSummaryDto summary = new PlanningSummaryDto(
        10L,
        20.0,
        2.0,
        8.0,
        2,
        List.of(new DomainPlanningMetricsDto(Domain.DEV, 20.0, 2.0, 8.0, 2, 18.0)),
        RiskLevel.LOW);
    when(planningService.calculateSummary("pod-1", 10L)).thenReturn(summary);

    mockMvc.perform(get("/api/v1/pods/pod-1/sprints/10/planning/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.totalAvailableCapacity").value(20.0));
  }

  @Test
  void validateReturnsEnvelope() throws Exception {
    when(planningService.validate("pod-1", 10L))
        .thenReturn(new PlanningValidationResultDto(List.of(), RiskLevel.LOW));

    mockMvc.perform(post("/api/v1/pods/pod-1/sprints/10/planning/validate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.riskLevel").value("LOW"));
  }
}
