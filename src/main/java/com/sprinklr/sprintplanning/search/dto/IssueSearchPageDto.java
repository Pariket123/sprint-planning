package com.sprinklr.sprintplanning.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated issue search results from live Jira")
public record IssueSearchPageDto(
    List<TicketViewDto> issues,
    int startAt,
    int maxResults,
    int total,
    boolean last
) {
  public static IssueSearchPageDto empty(int startAt, int maxResults) {
    return new IssueSearchPageDto(List.of(), startAt, maxResults, 0, true);
  }
}
