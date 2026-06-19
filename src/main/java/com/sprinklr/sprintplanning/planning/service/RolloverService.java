package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.planning.dto.RecordRolloverRequest;
import com.sprinklr.sprintplanning.planning.dto.RolloverIssueDto;

import java.util.List;

public interface RolloverService {

  RolloverIssueDto recordRollover(String podId, Long fromSprintId, RecordRolloverRequest request);

  List<RolloverIssueDto> getRolloverRecords(String podId, Long jiraSprintId);

  List<RolloverIssueDto> getOutgoingRollovers(String podId, Long jiraSprintId);

  List<RolloverIssueDto> getIncomingRollovers(String podId, Long jiraSprintId);
}
