package com.sprinklr.sprintplanning.common.model;

import com.sprinklr.sprintplanning.common.enums.Domain;

public record DomainAllocation(
    Domain domain,
    double storyPoints,
    boolean completed
) {
}
