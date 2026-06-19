package com.sprinklr.sprintplanning.client.jira.dto;

import java.util.List;

public record JiraIssueMoveRequest(List<String> issues) {
}
