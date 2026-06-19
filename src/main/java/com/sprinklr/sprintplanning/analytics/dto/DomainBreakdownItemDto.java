package com.sprinklr.sprintplanning.analytics.dto;

import com.sprinklr.sprintplanning.common.enums.Domain;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Story points and issue counts per domain")
public record DomainBreakdownItemDto(
    Domain domain,
    int count,
    double storyPoints
) {
}
