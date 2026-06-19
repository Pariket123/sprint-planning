package com.sprinklr.sprintplanning.team.mapper;

import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.team.model.PodJiraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface JiraConfigMapper {

  @Mapping(target = "storyPointsFieldId", source = "fieldMappings.storyPoints")
  @Mapping(target = "domainFieldId", source = "fieldMappings.domain")
  @Mapping(target = "domainValues", source = "fieldMappings.domainValues", qualifiedByName = "nullSafeMap")
  @Mapping(target = "bugIssueTypes", source = "issueTypeMappings.bugs", qualifiedByName = "nullSafeList")
  @Mapping(target = "featureIssueTypes", source = "issueTypeMappings.features", qualifiedByName = "nullSafeList")
  JiraFieldConfig toJiraFieldConfig(PodJiraConfig config);

  @Named("nullSafeMap")
  default Map<String, String> nullSafeMap(Map<String, String> map) {
    return map != null ? map : Collections.emptyMap();
  }

  @Named("nullSafeList")
  default List<String> nullSafeList(List<String> list) {
    return list != null ? list : Collections.emptyList();
  }
}
