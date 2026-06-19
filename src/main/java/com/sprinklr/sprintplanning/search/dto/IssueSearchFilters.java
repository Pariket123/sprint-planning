package com.sprinklr.sprintplanning.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Dynamic issue search filters")
public record IssueSearchFilters(
    List<String> issueTypes,
    List<String> statuses,
    List<String> domains,
    List<Long> sprintIds,
    List<String> fixVersions,
    List<String> fixVersionExcludes,
    List<String> assigneeIds,
    List<String> priorities,
    List<String> issueKeys,
    List<String> podIds,
    List<String> labels,
    List<String> components
) {
  public static IssueSearchFilters empty() {
    return new IssueSearchFilters(null, null, null, null, null, null, null, null, null, null, null, null);
  }
}
