package com.sprinklr.sprintplanning.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Count and story points for a category")
public record CategoryMetricsDto(
    int count,
    double storyPoints
) {
}
