package com.sprinklr.sprintplanning.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JQL function reference for autocomplete")
public record JqlFunctionReferenceDto(
    String value,
    String displayName
) {
}
