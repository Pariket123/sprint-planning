package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.common.enums.Domain;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Recorded rollover of an issue between sprints")
public record RolloverIssueDto(
    String issueKey,
    Long fromSprintId,
    Long toSprintId,
    String fromSprintName,
    String toSprintName,
    String statusAtRollover,
    Double storyPointsAtRollover,
    Domain domain,
    String domainLabel,
    Instant rolledOverAt,
    String rolledOverBy,
    String notes
) {
}
