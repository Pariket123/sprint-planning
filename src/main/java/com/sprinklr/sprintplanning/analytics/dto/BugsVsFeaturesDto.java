package com.sprinklr.sprintplanning.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Bugs, stories, and other issue-type breakdown")
public record BugsVsFeaturesDto(
    CategoryMetricsDto bugs,
    CategoryMetricsDto stories,
    List<IssueTypeMetricsDto> otherTypes
) {
}
