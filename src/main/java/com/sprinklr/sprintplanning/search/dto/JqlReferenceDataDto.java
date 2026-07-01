package com.sprinklr.sprintplanning.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "JQL reference metadata for building queries")
public record JqlReferenceDataDto(
    List<String> jqlReservedWords,
    List<JqlFieldReferenceDto> visibleFieldNames,
    List<JqlFunctionReferenceDto> visibleFunctionNames
) {
}
