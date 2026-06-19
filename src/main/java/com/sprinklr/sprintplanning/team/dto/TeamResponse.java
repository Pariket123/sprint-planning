package com.sprinklr.sprintplanning.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Team summary")
public record TeamResponse(
    @Schema(example = "64f1a2b3c4d5e6f7a8b9c0d1") String id,
    @Schema(example = "WFM") String code,
    @Schema(example = "Workforce Management") String name,
    boolean active
) {
}
