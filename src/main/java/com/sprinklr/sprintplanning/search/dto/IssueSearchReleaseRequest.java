package com.sprinklr.sprintplanning.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Additional JQL to narrow a release issue search")
public record IssueSearchReleaseRequest(
    @Schema(description = "Optional JQL intersected with the release base JQL using AND")
    String additionalJql
) {
}
