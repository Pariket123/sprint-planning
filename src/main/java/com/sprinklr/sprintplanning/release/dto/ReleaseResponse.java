package com.sprinklr.sprintplanning.release.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Release configuration for a pod/module")
public record ReleaseResponse(
    String id,
    String teamId,
    String podId,
    String name,
    String description,
    String baseJql,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
