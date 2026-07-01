package com.sprinklr.sprintplanning.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Domain capacity split across roadmap and bug/support work")
public record CapacityAllocationTableDto(
    List<CapacityAllocationRowDto> rows
) {
}
