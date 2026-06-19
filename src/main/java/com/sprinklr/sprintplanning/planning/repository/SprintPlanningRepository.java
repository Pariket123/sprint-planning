package com.sprinklr.sprintplanning.planning.repository;

import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SprintPlanningRepository extends MongoRepository<SprintPlanningDocument, String> {

  Optional<SprintPlanningDocument> findByPodIdAndJiraSprintId(String podId, Long jiraSprintId);
}
