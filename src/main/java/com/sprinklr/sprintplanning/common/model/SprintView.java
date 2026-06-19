package com.sprinklr.sprintplanning.common.model;

import java.time.Instant;

public record SprintView(
    Long id,
    String name,
    String state,
    Instant startDate,
    Instant endDate,
    String goal
) {
}
