package com.sprinklr.sprintplanning.release.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "Update release request for a pod/module")
public record UpdateReleaseRequest(
    @NotBlank String name,
    String description,
    List<String> fixVersionIncludes,
    List<String> fixVersionExcludes,
    @Valid ReleaseBasicFiltersDto basicFilters
) {
}
