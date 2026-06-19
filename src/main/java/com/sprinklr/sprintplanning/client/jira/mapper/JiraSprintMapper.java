package com.sprinklr.sprintplanning.client.jira.mapper;

import com.sprinklr.sprintplanning.client.jira.dto.JiraSprintDto;
import com.sprinklr.sprintplanning.common.model.SprintView;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JiraSprintMapper {

  SprintView toSprintView(JiraSprintDto sprint);

  List<SprintView> toSprintViews(List<JiraSprintDto> sprints);
}
