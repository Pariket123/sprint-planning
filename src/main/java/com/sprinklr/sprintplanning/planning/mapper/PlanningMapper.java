package com.sprinklr.sprintplanning.planning.mapper;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.planning.dto.PlanningDataDto;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import org.mapstruct.Mapper;

import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PlanningMapper {

  PlanningDataDto toPlanningDataDto(SprintPlanningDocument document);

  default Map<String, Double> toResolvedRolloverMap(Map<Domain, Double> rollover) {
    if (rollover == null) {
      return Map.of();
    }
    return rollover.entrySet().stream()
        .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue));
  }
}
