package com.sprinklr.sprintplanning.common.model;

import com.sprinklr.sprintplanning.search.dto.TicketViewDto;

import java.util.List;

public record IssueSearchPage(
    List<TicketViewDto> issues,
    int startAt,
    int maxResults,
    int total,
    boolean last
) {
}
