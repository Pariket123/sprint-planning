package com.sprinklr.sprintplanning.search.dto;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.enums.StatusCategory;
import com.sprinklr.sprintplanning.common.model.DomainAllocation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Normalized Jira issue details for search and planning views")
public record TicketViewDto(
    String key,
    String summary,
    String issueType,
    String status,
    StatusCategory statusCategory,
    Double storyPoints,
    Domain domain,
    List<DomainAllocation> domainAllocations,
    String assigneeId,
    String assigneeDisplayName,
    String priority,
    List<String> fixVersions,
    List<Long> sprintIds,
    List<String> labels,
    List<String> components
) {
}
