package com.sprinklr.sprintplanning.release.service;

import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.common.exception.ResourceNotFoundException;
import com.sprinklr.sprintplanning.release.dto.CreateReleaseRequest;
import com.sprinklr.sprintplanning.release.dto.ReleaseResponse;
import com.sprinklr.sprintplanning.release.dto.UpdateReleaseRequest;
import com.sprinklr.sprintplanning.release.mapper.ReleaseMapper;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.release.repository.ReleaseConfigRepository;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceImplTest {

  @Mock
  private TeamService teamService;
  @Mock
  private ReleaseConfigRepository releaseConfigRepository;

  private ReleaseService releaseService;

  @BeforeEach
  void setUp() {
    ReleaseMapper releaseMapper = Mappers.getMapper(ReleaseMapper.class);
    releaseService = new ReleaseServiceImpl(teamService, releaseConfigRepository, releaseMapper);
  }

  @Test
  void createReleaseStoresBaseJql() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod("pod-1", "team-1"));
    when(releaseConfigRepository.save(any())).thenAnswer(invocation -> {
      ReleaseConfigDocument saved = invocation.getArgument(0);
      saved.setId("release-1");
      return saved;
    });

    CreateReleaseRequest request = new CreateReleaseRequest(
        "Q3 2026",
        "Planning release for Q3",
        "project = CARE AND fixVersion = \"Q3 2026\"");

    ReleaseResponse response = releaseService.createRelease("pod-1", request);

    assertThat(response.podId()).isEqualTo("pod-1");
    assertThat(response.teamId()).isEqualTo("team-1");
    assertThat(response.baseJql()).isEqualTo("project = CARE AND fixVersion = \"Q3 2026\"");

    ArgumentCaptor<ReleaseConfigDocument> captor = ArgumentCaptor.forClass(ReleaseConfigDocument.class);
    verify(releaseConfigRepository).save(captor.capture());
    assertThat(captor.getValue().getBaseJql())
        .isEqualTo("project = CARE AND fixVersion = \"Q3 2026\"");
    assertThat(captor.getValue().getPodId()).isEqualTo("pod-1");
  }

  @Test
  void listReleasesReturnsActiveReleasesForPod() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod("pod-1", "team-1"));

    ReleaseConfigDocument release = activeRelease("release-1", "pod-1", "team-1", "Q3 2026");
    when(releaseConfigRepository.findByPodIdAndActiveTrueOrderByNameAsc("pod-1"))
        .thenReturn(List.of(release));

    List<ReleaseResponse> releases = releaseService.listReleases("pod-1");

    assertThat(releases).hasSize(1);
    assertThat(releases.get(0).name()).isEqualTo("Q3 2026");
  }

  @Test
  void updateReleaseUpdatesBaseJql() {
    ReleaseConfigDocument existing = activeRelease("release-1", "pod-1", "team-1", "Old Name");
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod("pod-1", "team-1"));
    when(releaseConfigRepository.findByIdAndPodIdAndActiveTrue("release-1", "pod-1"))
        .thenReturn(Optional.of(existing));
    when(releaseConfigRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateReleaseRequest request = new UpdateReleaseRequest(
        "Q4 2026",
        "Updated",
        "project = CARE AND fixVersion = \"Q4 2026\"");

    ReleaseResponse response = releaseService.updateRelease("pod-1", "release-1", request);

    assertThat(response.name()).isEqualTo("Q4 2026");
    assertThat(response.baseJql()).isEqualTo("project = CARE AND fixVersion = \"Q4 2026\"");
  }

  @Test
  void deactivateReleaseSetsActiveFalse() {
    ReleaseConfigDocument existing = activeRelease("release-1", "pod-1", "team-1", "Q3 2026");
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod("pod-1", "team-1"));
    when(releaseConfigRepository.findByIdAndPodIdAndActiveTrue("release-1", "pod-1"))
        .thenReturn(Optional.of(existing));
    when(releaseConfigRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ReleaseResponse response = releaseService.deactivateRelease("pod-1", "release-1");

    assertThat(response.active()).isFalse();
    assertThat(existing.isActive()).isFalse();
  }

  @Test
  void getReleaseThrowsWhenNotFoundForPod() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod("pod-1", "team-1"));
    when(releaseConfigRepository.findByIdAndPodIdAndActiveTrue("missing", "pod-1"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> releaseService.getRelease("pod-1", "missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void createReleaseRejectsBlankName() {
    when(teamService.getActivePodDocument("pod-1")).thenReturn(pod("pod-1", "team-1"));

    CreateReleaseRequest request = new CreateReleaseRequest("  ", null, "project = SCRUM");

    assertThatThrownBy(() -> releaseService.createRelease("pod-1", request))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("name is required");
  }

  private PodDocument pod(String podId, String teamId) {
    PodDocument pod = new PodDocument();
    pod.setId(podId);
    pod.setTeamId(teamId);
    return pod;
  }

  private ReleaseConfigDocument activeRelease(String id, String podId, String teamId, String name) {
    ReleaseConfigDocument release = new ReleaseConfigDocument();
    release.setId(id);
    release.setPodId(podId);
    release.setTeamId(teamId);
    release.setName(name);
    release.setBaseJql("project = SCRUM");
    release.setActive(true);
    release.setCreatedAt(Instant.now());
    release.setUpdatedAt(Instant.now());
    return release;
  }
}
