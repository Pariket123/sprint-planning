package com.sprinklr.sprintplanning.common.model;

import java.util.List;

public record BacklogPage(
    List<IssueView> issues,
    int startAt,
    int maxResults,
    int total,
    boolean last
) {
}
