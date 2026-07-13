package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;

public record PodJiraContext(Long boardId, JiraFieldConfig fieldConfig) {
}
