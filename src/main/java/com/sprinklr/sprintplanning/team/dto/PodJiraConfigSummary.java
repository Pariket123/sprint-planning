package com.sprinklr.sprintplanning.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Jira configuration summary for a pod")
public record PodJiraConfigSummary(
    Long boardId,
    List<String> projectKeys,
    String storyPointsField,
    String domainField,
    Map<String, String> domainValues
) {
}
