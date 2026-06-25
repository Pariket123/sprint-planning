package com.sprinklr.sprintplanning.release.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Update release request for a pod/module")
public record UpdateReleaseRequest(
    @NotBlank String name,
    String description,
    @Schema(description = "Base JQL defining the release issue scope")
    String baseJql
) {
}
