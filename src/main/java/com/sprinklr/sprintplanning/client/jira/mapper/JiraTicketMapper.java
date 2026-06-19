package com.sprinklr.sprintplanning.client.jira.mapper;

import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.search.dto.TicketViewDto;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = JiraIssueMappingHelper.class)
public interface JiraTicketMapper {

  @Mapping(target = "key", source = "key")
  @Mapping(target = "summary", source = ".", qualifiedByName = "resolveSummary")
  @Mapping(target = "issueType", source = ".", qualifiedByName = "resolveIssueType")
  @Mapping(target = "status", source = ".", qualifiedByName = "resolveStatus")
  @Mapping(target = "statusCategory", source = ".", qualifiedByName = "resolveStatusCategory")
  @Mapping(target = "storyPoints", source = ".", qualifiedByName = "resolveStoryPoints")
  @Mapping(target = "domain", source = ".", qualifiedByName = "resolveDomain")
  @Mapping(target = "assigneeId", source = ".", qualifiedByName = "resolveAssigneeId")
  @Mapping(target = "assigneeDisplayName", source = ".", qualifiedByName = "resolveAssigneeDisplayName")
  @Mapping(target = "priority", source = ".", qualifiedByName = "resolvePriority")
  @Mapping(target = "fixVersions", source = ".", qualifiedByName = "resolveFixVersions")
  @Mapping(target = "sprintIds", source = ".", qualifiedByName = "resolveSprintIds")
  @Mapping(target = "labels", source = ".", qualifiedByName = "resolveLabels")
  @Mapping(target = "components", source = ".", qualifiedByName = "resolveComponents")
  TicketViewDto toTicketView(JiraIssueDto issue, @Context JiraFieldConfig config);

  List<TicketViewDto> toTicketViews(List<JiraIssueDto> issues, @Context JiraFieldConfig config);
}
