package com.sprinklr.sprintplanning.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Engineering sub-domain completion within the dev-or-beyond issue pool")
public record DevSubDomainMetricsDto(
    int subDomainPoolIssueCount,
    List<DevSubDomainItemDto> subDomains
) {
}
