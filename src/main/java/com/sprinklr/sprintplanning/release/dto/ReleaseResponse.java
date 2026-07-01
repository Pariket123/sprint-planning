package com.sprinklr.sprintplanning.release.dto;

import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Team-scoped release configuration")
public record ReleaseResponse(
    String id,
    String teamId,
    String name,
    String description,
    String baseJql,
    Integer durationDays,
    LocalDate startDate,
    List<PersonCapacity> capacity,
    Double leavePercent,
    List<CapacityAllocationPercents> capacityAllocation,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
