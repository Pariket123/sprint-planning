package com.sprinklr.sprintplanning.planning.dto;

import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Request to upsert per-domain capacity inputs")
public record UpdateCapacityRequest(
    @NotNull @Valid List<PersonCapacity> capacity
) {
}
