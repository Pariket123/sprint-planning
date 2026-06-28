package com.sprinklr.sprintplanning.common.model;

import java.util.List;

public record WorkflowAnalysisSection(
    String key,
    String label,
    List<String> statuses
) {
}
