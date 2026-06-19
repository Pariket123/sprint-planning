package com.sprinklr.sprintplanning.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Record rollover of an issue from one sprint to another")
public record RecordRolloverRequest(
    @NotBlank String issueKey,
    @NotNull Long toSprintId,
    String notes,
    Boolean moveInJira
) {
  public boolean shouldMoveInJira() {
    return moveInJira != null && moveInJira;
  }
}
