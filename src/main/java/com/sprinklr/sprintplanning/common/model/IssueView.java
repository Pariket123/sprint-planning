package com.sprinklr.sprintplanning.common.model;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;

public record IssueView(
        String key,
        String summary,
        Domain domain,
        Double storyPoints,
        String issueType,
        String status,
        StatusCategory statusCategory
) {
}
