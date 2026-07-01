package com.sprinklr.sprintplanning.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "JQL field reference for autocomplete")
public record JqlFieldReferenceDto(
    String value,
    String displayName,
    String cfid,
    List<String> operators
) {
}
