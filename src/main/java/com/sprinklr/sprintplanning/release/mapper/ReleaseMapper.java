package com.sprinklr.sprintplanning.release.mapper;

import com.sprinklr.sprintplanning.release.dto.ReleaseResponse;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReleaseMapper {

  ReleaseResponse toReleaseResponse(ReleaseConfigDocument document);

  List<ReleaseResponse> toReleaseResponses(List<ReleaseConfigDocument> documents);
}
