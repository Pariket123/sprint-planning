package com.sprinklr.sprintplanning.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error details returned when success is false")
public record ErrorDetail(
        @Schema(description = "Stable machine-readable error code", example = "POD_NOT_FOUND")
        String code,
        @Schema(description = "Human-readable error message")
        String message
) {
}
