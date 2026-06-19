package com.sprinklr.sprintplanning.release.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Release configuration for a pod/module")
public record ReleaseResponse(
    String id,
    String teamId,
    String podId,
    String name,
    String description,
    List<String> fixVersionIncludes,
    List<String> fixVersionExcludes,
    ReleaseBasicFiltersDto basicFilters,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
