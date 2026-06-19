package com.sprinklr.sprintplanning.planning.repository;

import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SprintPlanningRepository extends MongoRepository<SprintPlanningDocument, String> {

  Optional<SprintPlanningDocument> findByPodIdAndJiraSprintId(String podId, Long jiraSprintId);

  @Query("{ 'podId': ?0, 'rolloverIssues.toSprintId': ?1 }")
  List<SprintPlanningDocument> findIncomingRollovers(String podId, Long toSprintId);
}
