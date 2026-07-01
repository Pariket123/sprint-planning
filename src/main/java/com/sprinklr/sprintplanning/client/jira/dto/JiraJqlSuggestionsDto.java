package com.sprinklr.sprintplanning.client.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraJqlSuggestionsDto {

  private List<JiraJqlSuggestionItemDto> results = new ArrayList<>();

  public List<JiraJqlSuggestionItemDto> getResults() {
    return results;
  }

  public void setResults(List<JiraJqlSuggestionItemDto> results) {
    this.results = results != null ? results : new ArrayList<>();
  }
}
