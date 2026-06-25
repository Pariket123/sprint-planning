package com.sprinklr.sprintplanning.release.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

@Schema(description = "Create release request for a pod/module")
public record CreateReleaseRequest(
    @NotBlank String name,
    String description,
    @Schema(description = "Base JQL defining the release issue scope")
    String baseJql,
    @Schema(description = "Number of working days in the release window for capacity planning")
    @Min(1)
    Integer durationDays,
    @Schema(description = "Release window start date used for leave overlap calculations")
    LocalDate startDate
) {
}
