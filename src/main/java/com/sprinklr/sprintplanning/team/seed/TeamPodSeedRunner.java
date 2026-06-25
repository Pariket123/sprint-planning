package com.sprinklr.sprintplanning.team.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.model.PodJiraConfig;
import com.sprinklr.sprintplanning.team.model.TeamDocument;
import com.sprinklr.sprintplanning.team.repository.PodRepository;
import com.sprinklr.sprintplanning.team.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class TeamPodSeedRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(TeamPodSeedRunner.class);

  private final TeamRepository teamRepository;
  private final PodRepository podRepository;
  private final ObjectMapper objectMapper;

  public TeamPodSeedRunner(
      TeamRepository teamRepository,
      PodRepository podRepository,
      ObjectMapper objectMapper) {
    this.teamRepository = teamRepository;
    this.podRepository = podRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    try (InputStream inputStream = new ClassPathResource("data/teams-pods.json").getInputStream()) {
      JsonNode root = objectMapper.readTree(inputStream);
      if (teamRepository.count() == 0) {
        seedTeamsAndPods(root);
        log.info("Seeded teams and pods from teams-pods.json");
      } else {
        syncJiraFieldMappings(root);
      }
    }
  }

  private void seedTeamsAndPods(JsonNode root) throws Exception {
    Iterator<JsonNode> teams = root.path("teams").elements();

    while (teams.hasNext()) {
      JsonNode teamNode = teams.next();
      TeamDocument team = new TeamDocument();
      team.setCode(teamNode.path("code").asText());
      team.setName(teamNode.path("name").asText());
      team.setActive(teamNode.path("active").asBoolean(true));
      team = teamRepository.save(team);

      Iterator<JsonNode> pods = teamNode.path("pods").elements();
      while (pods.hasNext()) {
        JsonNode podNode = pods.next();
        PodDocument pod = new PodDocument();
        pod.setTeamId(team.getId());
        pod.setCode(podNode.path("code").asText());
        pod.setName(podNode.path("name").asText());
        pod.setActive(podNode.path("active").asBoolean(true));
        pod.setJiraConfig(objectMapper.treeToValue(podNode.path("jiraConfig"), PodJiraConfig.class));
        podRepository.save(pod);
      }
    }
  }

  private void syncJiraFieldMappings(JsonNode root) throws Exception {
    int updatedPods = 0;
    Iterator<JsonNode> teams = root.path("teams").elements();

    while (teams.hasNext()) {
      JsonNode teamNode = teams.next();
      String teamCode = teamNode.path("code").asText();
      Optional<TeamDocument> team = teamRepository.findByCode(teamCode);
      if (team.isEmpty()) {
        continue;
      }

      Iterator<JsonNode> pods = teamNode.path("pods").elements();
      while (pods.hasNext()) {
        JsonNode podNode = pods.next();
        String podCode = podNode.path("code").asText();
        Optional<PodDocument> existingPod =
            podRepository.findByTeamIdAndCode(team.get().getId(), podCode);
        if (existingPod.isEmpty()) {
          continue;
        }

        PodJiraConfig seedConfig = objectMapper.treeToValue(podNode.path("jiraConfig"), PodJiraConfig.class);
        PodDocument pod = existingPod.get();
        PodJiraConfig currentConfig = pod.getJiraConfig() != null ? pod.getJiraConfig() : new PodJiraConfig();
        if (mergeFieldMappings(currentConfig, seedConfig)) {
          pod.setJiraConfig(currentConfig);
          podRepository.save(pod);
          updatedPods++;
        }
      }
    }

    if (updatedPods > 0) {
      log.info("Synced Jira field mappings for {} pod(s) from teams-pods.json", updatedPods);
    } else {
      log.debug("No pod Jira field mappings required syncing");
    }
  }

  private boolean mergeFieldMappings(PodJiraConfig currentConfig, PodJiraConfig seedConfig) throws Exception {
    if (seedConfig == null || seedConfig.getFieldMappings() == null) {
      return false;
    }

    PodJiraConfig.FieldMappings seedMappings = seedConfig.getFieldMappings();
    PodJiraConfig.FieldMappings currentMappings = currentConfig.getFieldMappings();
    if (currentMappings == null) {
      currentMappings = new PodJiraConfig.FieldMappings();
      currentConfig.setFieldMappings(currentMappings);
    }

    boolean changed = false;
    changed |= updateIfDifferent(currentMappings::getStoryPoints, currentMappings::setStoryPoints, seedMappings.getStoryPoints());
    changed |= updateIfDifferent(currentMappings::getDomain, currentMappings::setDomain, seedMappings.getDomain());
    changed |= updateIfDifferent(currentMappings::getSprint, currentMappings::setSprint, seedMappings.getSprint());
    if (seedMappings.getDomainValues() != null && !seedMappings.getDomainValues().equals(currentMappings.getDomainValues())) {
      currentMappings.setDomainValues(seedMappings.getDomainValues());
      changed = true;
    }
    changed |= updateMapIfDifferent(
        currentMappings::getCompositeDomainValues,
        currentMappings::setCompositeDomainValues,
        seedMappings.getCompositeDomainValues());
    changed |= updateMapIfDifferent(
        currentMappings::getDomainStoryPointFields,
        currentMappings::setDomainStoryPointFields,
        seedMappings.getDomainStoryPointFields());
    changed |= updateIfDifferent(
        currentMappings::getDomainCompletionField,
        currentMappings::setDomainCompletionField,
        seedMappings.getDomainCompletionField());
    changed |= updateMapIfDifferent(
        currentMappings::getDomainCompletionValues,
        currentMappings::setDomainCompletionValues,
        seedMappings.getDomainCompletionValues());
    return changed;
  }

  private boolean updateMapIfDifferent(
      Supplier<Map<String, String>> getter,
      Consumer<Map<String, String>> setter,
      Map<String, String> seedValue) {
    if (seedValue == null || seedValue.isEmpty()) {
      return false;
    }
    Map<String, String> currentValue = getter.get();
    if (seedValue.equals(currentValue)) {
      return false;
    }
    setter.accept(seedValue);
    return true;
  }

  private boolean updateIfDifferent(
      Supplier<String> getter,
      Consumer<String> setter,
      String seedValue) {
    if (seedValue == null || seedValue.isBlank() || seedValue.equals(getter.get())) {
      return false;
    }
    setter.accept(seedValue);
    return true;
  }
}
