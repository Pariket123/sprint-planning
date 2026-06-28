package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.common.model.IssueView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated sprint and selected issues for the planning issues tab")
public record PlanningIssuesPageDto(
    List<IssueView> sprintIssues,
    List<IssueView> selectedIssues,
    int startAt,
    int maxResults,
    int sprintIssueTotal,
    boolean last
) {
}
