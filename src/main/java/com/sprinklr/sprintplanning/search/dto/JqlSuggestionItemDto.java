package com.sprinklr.sprintplanning.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JQL autocomplete suggestion item")
public record JqlSuggestionItemDto(
    String value,
    String displayName
) {
}
