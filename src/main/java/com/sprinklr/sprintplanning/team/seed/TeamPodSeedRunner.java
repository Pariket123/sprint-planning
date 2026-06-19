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
    if (teamRepository.count() > 0) {
      log.debug("Teams already seeded, skipping");
      return;
    }

    try (InputStream inputStream = new ClassPathResource("data/teams-pods.json").getInputStream()) {
      JsonNode root = objectMapper.readTree(inputStream);
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

      log.info("Seeded teams and pods from teams-pods.json");
    }
  }
}
