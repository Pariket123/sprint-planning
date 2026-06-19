package com.sprinklr.sprintplanning.release.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Basic release-level issue filters")
public record ReleaseBasicFiltersDto(
    List<String> issueTypes,
    List<String> statuses,
    List<String> domains,
    List<String> priorities,
    List<String> assigneeIds
) {
}
