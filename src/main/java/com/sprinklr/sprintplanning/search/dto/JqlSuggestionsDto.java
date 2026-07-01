package com.sprinklr.sprintplanning.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "JQL autocomplete suggestions for a field value")
public record JqlSuggestionsDto(
    List<JqlSuggestionItemDto> results
) {
  public static JqlSuggestionsDto empty() {
    return new JqlSuggestionsDto(List.of());
  }
}
