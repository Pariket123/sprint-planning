package com.sprinklr.sprintplanning.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Bulk issue key request")
public record IssueKeysRequest(@NotEmpty List<String> issueKeys) {
}
