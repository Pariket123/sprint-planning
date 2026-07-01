package com.sprinklr.sprintplanning.search.controller;

import com.sprinklr.sprintplanning.TestSecurityConfig;
import com.sprinklr.sprintplanning.analytics.dto.AnalyticsResponse;
import com.sprinklr.sprintplanning.analytics.dto.BugsVsFeaturesDto;
import com.sprinklr.sprintplanning.analytics.dto.CategoryMetricsDto;
import com.sprinklr.sprintplanning.analytics.dto.IssueCountsDto;
import com.sprinklr.sprintplanning.search.dto.IssueSearchPageDto;
import com.sprinklr.sprintplanning.search.service.IssueSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IssueSearchController.class)
@Import({TestSecurityConfig.class, com.sprinklr.sprintplanning.common.handler.GlobalExceptionHandler.class})
class IssueSearchControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private IssueSearchService issueSearchService;

  @Test
  void searchInPodReturnsEnvelope() throws Exception {
    when(issueSearchService.searchInPod(eq("pod-1"), any(), eq(0), eq(50)))
        .thenReturn(new IssueSearchPageDto(List.of(), 0, 50, 0, true));

    mockMvc.perform(post("/api/v1/pods/pod-1/issues/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "issueTypes": ["Story"],
                  "statuses": ["In Progress"],
                  "domains": ["BE"]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.last").value(true));

    verify(issueSearchService).searchInPod(eq("pod-1"), any(), eq(0), eq(50));
  }

  @Test
  void searchInReleaseReturnsEnvelope() throws Exception {
    when(issueSearchService.searchInRelease(eq("pod-1"), eq("release-1"), any(), eq(0), eq(50)))
        .thenReturn(new IssueSearchPageDto(List.of(), 0, 50, 2, false));

    mockMvc.perform(post("/api/v1/pods/pod-1/releases/release-1/issues/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "fixVersions": ["Q3 2026"] }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.total").value(2));

    verify(issueSearchService).searchInRelease(eq("pod-1"), eq("release-1"), any(), eq(0), eq(50));
  }

  @Test
  void analyzeReleaseIssuesReturnsEnvelope() throws Exception {
    AnalyticsResponse analytics = new AnalyticsResponse(
        null,
        "Q3 Release",
        8.0,
        5.0,
        3.0,
        new IssueCountsDto(2, 1, 1),
        new BugsVsFeaturesDto(
            new CategoryMetricsDto(1, 3.0),
            new CategoryMetricsDto(1, 5.0),
            List.of()),
        List.of(),
        List.of(),
        null,
        null);

    when(issueSearchService.analyzeRelease(eq("pod-1"), eq("release-1"), any()))
        .thenReturn(analytics);

    mockMvc.perform(post("/api/v1/pods/pod-1/releases/release-1/issues/analytics")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "additionalJql": "status != Done" }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sprintName").value("Q3 Release"))
        .andExpect(jsonPath("$.data.issueCounts.total").value(2));

    verify(issueSearchService).analyzeRelease(eq("pod-1"), eq("release-1"), any());
  }
}
