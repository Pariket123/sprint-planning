package com.sprinklr.sprintplanning.analytics.controller;

import com.sprinklr.sprintplanning.TestSecurityConfig;
import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.analytics.dto.BugsVsFeaturesDto;
import com.sprinklr.sprintplanning.analytics.dto.CategoryMetricsDto;
import com.sprinklr.sprintplanning.analytics.dto.DomainBreakdownItemDto;
import com.sprinklr.sprintplanning.analytics.dto.IssueCountsDto;
import com.sprinklr.sprintplanning.analytics.dto.StatusDistributionItemDto;
import com.sprinklr.sprintplanning.analytics.service.AnalyticsService;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.SprintView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
@Import({TestSecurityConfig.class, com.sprinklr.sprintplanning.common.handler.GlobalExceptionHandler.class})
class AnalyticsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AnalyticsService analyticsService;

  @Test
  void listSprintsReturnsEnvelope() throws Exception {
    when(analyticsService.getSprints("pod-1", "active,future,closed"))
        .thenReturn(List.of(new SprintView(10L, "Sprint 10", "active",
            Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), null)));

    mockMvc.perform(get("/api/v1/pods/pod-1/sprints"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].name").value("Sprint 10"));
  }

  @Test
  void getSprintAnalyticsReturnsEnvelope() throws Exception {
    AnalyticsResponse analytics = new AnalyticsResponse(
        10L,
        "Sprint 10",
        13.0,
        5.0,
        8.0,
        new IssueCountsDto(3, 1, 2),
        new BugsVsFeaturesDto(
            new CategoryMetricsDto(1, 3.0),
            new CategoryMetricsDto(2, 10.0),
            new CategoryMetricsDto(0, 0.0)),
        List.of(new StatusDistributionItemDto("Done", StatusCategory.DONE, 1, 5.0)),
        List.of(new DomainBreakdownItemDto(
            Domain.DEV, 2, 10.0, 66.67, 76.92, 1, 5.0, 1, 5.0, 50.0, 50.0)),
        null,
        null);

    when(analyticsService.getSprintAnalytics("pod-1", 10L)).thenReturn(analytics);

    mockMvc.perform(get("/api/v1/pods/pod-1/sprints/10/analytics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.totalStoryPoints").value(13.0))
        .andExpect(jsonPath("$.data.issueCounts.completed").value(1))
        .andExpect(jsonPath("$.data.bugsVsFeatures.bugs.count").value(1))
        .andExpect(jsonPath("$.data.domainBreakdown[0].issueCountPercentage").value(66.67))
        .andExpect(jsonPath("$.data.domainBreakdown[0].storyPointCompletionPercentage").value(50.0));
  }
}
