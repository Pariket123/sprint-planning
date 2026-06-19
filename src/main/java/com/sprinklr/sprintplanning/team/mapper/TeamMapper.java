package com.sprinklr.sprintplanning.team.mapper;

import com.sprinklr.sprintplanning.team.dto.PodJiraConfigSummary;
import com.sprinklr.sprintplanning.team.dto.PodResponse;
import com.sprinklr.sprintplanning.team.dto.TeamResponse;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.model.PodJiraConfig;
import com.sprinklr.sprintplanning.team.model.TeamDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface TeamMapper {

  TeamResponse toTeamResponse(TeamDocument team);

  List<TeamResponse> toTeamResponses(List<TeamDocument> teams);

  @Mapping(target = "jiraConfig", source = "jiraConfig", qualifiedByName = "toJiraConfigSummary")
  PodResponse toPodResponse(PodDocument pod);

  List<PodResponse> toPodResponses(List<PodDocument> pods);

  @Named("toJiraConfigSummary")
  default PodJiraConfigSummary toJiraConfigSummary(PodJiraConfig config) {
    if (config == null) {
      return new PodJiraConfigSummary(null, List.of(), null, null, Map.of());
    }
    PodJiraConfig.FieldMappings mappings = config.getFieldMappings();
    Map<String, String> domainValues = mappings != null && mappings.getDomainValues() != null
        ? mappings.getDomainValues()
        : Collections.emptyMap();
    return new PodJiraConfigSummary(
        config.getBoardId(),
        config.getProjectKeys() != null ? config.getProjectKeys() : List.of(),
        mappings != null ? mappings.getStoryPoints() : null,
        mappings != null ? mappings.getDomain() : null,
        domainValues
    );
  }
}
