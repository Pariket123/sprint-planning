package com.sprinklr.sprintplanning.planning.controller;

import com.sprinklr.sprintplanning.TestSecurityConfig;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.planning.dto.RolloverIssueDto;
import com.sprinklr.sprintplanning.planning.service.RolloverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RolloverController.class)
@Import({TestSecurityConfig.class, com.sprinklr.sprintplanning.common.handler.GlobalExceptionHandler.class})
class RolloverControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private RolloverService rolloverService;

  @Test
  void recordRolloverReturnsEnvelope() throws Exception {
    when(rolloverService.recordRollover(eq("pod-1"), eq(12L), any()))
        .thenReturn(rolloverIssue());

    mockMvc.perform(post("/api/v1/pods/pod-1/sprints/12/rollover")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "issueKey": "CARE-105613",
                  "toSprintId": 13,
                  "notes": "Incomplete",
                  "moveInJira": false
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.issueKey").value("CARE-105613"));

    verify(rolloverService).recordRollover(eq("pod-1"), eq(12L), any());
  }

  @Test
  void getOutgoingRolloversReturnsEnvelope() throws Exception {
    when(rolloverService.getOutgoingRollovers("pod-1", 12L)).thenReturn(List.of(rolloverIssue()));

    mockMvc.perform(get("/api/v1/pods/pod-1/sprints/12/rollover/outgoing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].fromSprintId").value(12));
  }

  @Test
  void getIncomingRolloversReturnsEnvelope() throws Exception {
    when(rolloverService.getIncomingRollovers("pod-1", 13L)).thenReturn(List.of(rolloverIssue()));

    mockMvc.perform(get("/api/v1/pods/pod-1/sprints/13/rollover/incoming"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].toSprintId").value(13));
  }

  private RolloverIssueDto rolloverIssue() {
    return new RolloverIssueDto(
        "CARE-105613", 12L, 13L, "In Progress", 2.0, Domain.DEV, null,
        Instant.now(), null, "Incomplete");
  }
}
