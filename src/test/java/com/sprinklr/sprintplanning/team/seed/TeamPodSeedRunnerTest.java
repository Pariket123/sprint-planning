package com.sprinklr.sprintplanning.team.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.model.PodJiraConfig;
import com.sprinklr.sprintplanning.team.model.TeamDocument;
import com.sprinklr.sprintplanning.team.repository.PodRepository;
import com.sprinklr.sprintplanning.team.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamPodSeedRunnerTest {

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private PodRepository podRepository;

  private TeamPodSeedRunner seedRunner;

  @BeforeEach
  void setUp() {
    seedRunner = new TeamPodSeedRunner(teamRepository, podRepository, new ObjectMapper());
  }

  @Test
  void syncsMultiDomainFieldMappingsOnStartupWhenTeamsAlreadyExist() throws Exception {
    TeamDocument team = new TeamDocument();
    team.setId("team-1");
    team.setCode("WFM");

    PodDocument pod = new PodDocument();
    pod.setId("pod-1");
    pod.setTeamId("team-1");
    pod.setCode("FORECASTING");
    PodJiraConfig jiraConfig = new PodJiraConfig();
    PodJiraConfig.FieldMappings fieldMappings = new PodJiraConfig.FieldMappings();
    fieldMappings.setStoryPoints("customfield_10016");
    fieldMappings.setDomain("customfield_10109");
    fieldMappings.setSprint("customfield_10020");
    fieldMappings.setDomainValues(Map.of("BE", "Backend"));
    jiraConfig.setFieldMappings(fieldMappings);
    pod.setJiraConfig(jiraConfig);

    when(teamRepository.count()).thenReturn(1L);
    when(teamRepository.findByCode("WFM")).thenReturn(Optional.of(team));
    when(podRepository.findByTeamIdAndCode("team-1", "FORECASTING")).thenReturn(Optional.of(pod));
    when(podRepository.findByTeamIdAndCode("team-1", "REPORTING")).thenReturn(Optional.empty());
    when(podRepository.findByTeamIdAndCode("team-1", "CONFIGURATION")).thenReturn(Optional.empty());

    seedRunner.run(new DefaultApplicationArguments());

    ArgumentCaptor<PodDocument> podCaptor = ArgumentCaptor.forClass(PodDocument.class);
    verify(podRepository).save(podCaptor.capture());

    PodJiraConfig.FieldMappings synced = podCaptor.getValue().getJiraConfig().getFieldMappings();
    assertThat(synced.getCompositeDomainValues()).containsEntry("BE+UI", "Backend+Ui");
    assertThat(synced.getDomainStoryPointFields()).containsEntry("BE", "customfield_10144");
    assertThat(synced.getDomainCompletionField()).isEqualTo("customfield_10143");
    assertThat(synced.getDomainCompletionValues()).containsEntry("BE", "Be");
  }

  @Test
  void doesNotSavePodWhenFieldMappingsAlreadyMatchSeedFile() throws Exception {
    JsonNode forecastingPod =
        new ObjectMapper()
            .readTree(new ClassPathResource("data/teams-pods.json").getInputStream())
            .path("teams")
            .get(0)
            .path("pods")
            .get(0);
    PodJiraConfig seedConfig =
        new ObjectMapper().treeToValue(forecastingPod.path("jiraConfig"), PodJiraConfig.class);

    TeamDocument team = new TeamDocument();
    team.setId("team-1");
    team.setCode("WFM");

    PodDocument pod = new PodDocument();
    pod.setId("pod-1");
    pod.setTeamId("team-1");
    pod.setCode("FORECASTING");
    pod.setJiraConfig(seedConfig);

    when(teamRepository.count()).thenReturn(1L);
    when(teamRepository.findByCode("WFM")).thenReturn(Optional.of(team));
    when(podRepository.findByTeamIdAndCode("team-1", "FORECASTING")).thenReturn(Optional.of(pod));
    when(podRepository.findByTeamIdAndCode("team-1", "REPORTING")).thenReturn(Optional.empty());
    when(podRepository.findByTeamIdAndCode("team-1", "CONFIGURATION")).thenReturn(Optional.empty());

    seedRunner.run(new DefaultApplicationArguments());

    verify(podRepository, never()).save(any());
  }
}
