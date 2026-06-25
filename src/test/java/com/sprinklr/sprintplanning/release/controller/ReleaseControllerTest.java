package com.sprinklr.sprintplanning.release.controller;

import com.sprinklr.sprintplanning.TestSecurityConfig;
import com.sprinklr.sprintplanning.release.dto.ReleaseResponse;
import com.sprinklr.sprintplanning.release.service.ReleaseService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReleaseController.class)
@Import({TestSecurityConfig.class, com.sprinklr.sprintplanning.common.handler.GlobalExceptionHandler.class})
class ReleaseControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ReleaseService releaseService;

  @Test
  void listReleasesReturnsEnvelope() throws Exception {
    when(releaseService.listReleases("pod-1")).thenReturn(List.of(releaseResponse()));

    mockMvc.perform(get("/api/v1/pods/pod-1/releases"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].name").value("Q3 2026"));
  }

  @Test
  void createReleaseReturnsEnvelope() throws Exception {
    when(releaseService.createRelease(eq("pod-1"), any())).thenReturn(releaseResponse());

    mockMvc.perform(post("/api/v1/pods/pod-1/releases")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Q3 2026",
                  "description": "Planning release for Q3",
                  "baseJql": "project = CARE AND fixVersion = \\"Q3 2026\\""
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.podId").value("pod-1"));

    verify(releaseService).createRelease(eq("pod-1"), any());
  }

  @Test
  void updateReleaseReturnsEnvelope() throws Exception {
    when(releaseService.updateRelease(eq("pod-1"), eq("release-1"), any())).thenReturn(releaseResponse());

    mockMvc.perform(put("/api/v1/pods/pod-1/releases/release-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Q3 2026",
                  "baseJql": "project = CARE AND fixVersion = \\"Q3 2026\\""
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void deactivateReleaseReturnsEnvelope() throws Exception {
    ReleaseResponse deactivated = new ReleaseResponse(
        "release-1", "team-1", "pod-1", "Q3 2026", null,
        "project = CARE", 20, null, List.of(), 0.0, false, Instant.now(), Instant.now());
    when(releaseService.deactivateRelease("pod-1", "release-1")).thenReturn(deactivated);

    mockMvc.perform(delete("/api/v1/pods/pod-1/releases/release-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.active").value(false));
  }

  private ReleaseResponse releaseResponse() {
    return new ReleaseResponse(
        "release-1",
        "team-1",
        "pod-1",
        "Q3 2026",
        "Planning release for Q3",
        "project = CARE AND fixVersion = \"Q3 2026\"",
        20,
        null,
        List.of(),
        0.0,
        true,
        Instant.now(),
        Instant.now());
  }
}
