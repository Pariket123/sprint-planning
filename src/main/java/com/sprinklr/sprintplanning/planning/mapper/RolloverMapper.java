package com.sprinklr.sprintplanning.planning.mapper;

import com.sprinklr.sprintplanning.planning.dto.RolloverIssueDto;
import com.sprinklr.sprintplanning.planning.model.RolloverIssue;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RolloverMapper {

  public RolloverIssueDto toDto(RolloverIssue issue) {
    if (issue == null) {
      return null;
    }
    return new RolloverIssueDto(
        issue.getIssueKey(),
        issue.getFromSprintId(),
        issue.getToSprintId(),
        issue.getStatusAtRollover(),
        issue.getStoryPointsAtRollover(),
        issue.getDomain(),
        issue.getDomainLabel(),
        issue.getRolledOverAt(),
        issue.getRolledOverBy(),
        issue.getNotes());
  }

  public List<RolloverIssueDto> toDtos(List<RolloverIssue> issues) {
    if (issues == null) {
      return List.of();
    }
    return issues.stream().map(this::toDto).toList();
  }
}
