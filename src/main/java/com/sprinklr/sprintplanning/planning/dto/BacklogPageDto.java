package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.common.model.IssueView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated backlog issues from live Jira")
public record BacklogPageDto(
    List<IssueView> issues,
    int startAt,
    int maxResults,
    int total,
    boolean last
) {
}
