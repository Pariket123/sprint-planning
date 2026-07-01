package com.sprinklr.sprintplanning.common.model;

import java.util.List;

public record DevSubDomainIssueTypeProfile(
    String key,
    String label,
    List<String> issueTypes
) {
}
