package com.sprinklr.sprintplanning.release.dto;

import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Release configuration for a pod/module")
public record ReleaseResponse(
    String id,
    String teamId,
    String podId,
    String name,
    String description,
    String baseJql,
    Integer durationDays,
    LocalDate startDate,
    List<PersonCapacity> capacity,
    Double leavePercent,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
