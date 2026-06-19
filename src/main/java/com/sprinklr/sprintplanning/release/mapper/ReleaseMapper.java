package com.sprinklr.sprintplanning.release.mapper;

import com.sprinklr.sprintplanning.release.dto.ReleaseBasicFiltersDto;
import com.sprinklr.sprintplanning.release.dto.ReleaseResponse;
import com.sprinklr.sprintplanning.release.model.ReleaseBasicFilters;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReleaseMapper {

  @Mapping(target = "basicFilters", source = "basicFilters")
  ReleaseResponse toReleaseResponse(ReleaseConfigDocument document);

  ReleaseBasicFiltersDto toBasicFiltersDto(ReleaseBasicFilters basicFilters);

  List<ReleaseResponse> toReleaseResponses(List<ReleaseConfigDocument> documents);
}
