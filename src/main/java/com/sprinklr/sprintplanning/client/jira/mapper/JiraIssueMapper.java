package com.sprinklr.sprintplanning.client.jira.mapper;

import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = JiraIssueMappingHelper.class)
public interface JiraIssueMapper {

  @Mapping(target = "key", source = "key")
  @Mapping(target = "summary", source = ".", qualifiedByName = "resolveSummary")
  @Mapping(target = "issueType", source = ".", qualifiedByName = "resolveIssueType")
  @Mapping(target = "status", source = ".", qualifiedByName = "resolveStatus")
  @Mapping(target = "statusCategory", source = ".", qualifiedByName = "resolveStatusCategory")
  @Mapping(target = "storyPoints", source = ".", qualifiedByName = "resolveStoryPoints")
  @Mapping(target = "domain", source = ".", qualifiedByName = "resolveDomain")
  @Mapping(target = "domainAllocations", source = ".", qualifiedByName = "resolveDomainAllocations")
  IssueView toIssueView(JiraIssueDto issue, @Context JiraFieldConfig config);

  List<IssueView> toIssueViews(List<JiraIssueDto> issues, @Context JiraFieldConfig config);
}
