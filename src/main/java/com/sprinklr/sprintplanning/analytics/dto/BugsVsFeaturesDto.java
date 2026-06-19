package com.sprinklr.sprintplanning.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bugs vs features breakdown")
public record BugsVsFeaturesDto(
    CategoryMetricsDto bugs,
    CategoryMetricsDto features,
    CategoryMetricsDto other
) {
}
