package com.sprinklr.sprintplanning.planning.repository;

import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface SprintPlanningRepository extends MongoRepository<SprintPlanningDocument, String> {

  List<SprintPlanningDocument> findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc(
      String podId, Long jiraSprintId);

  @Query("{ 'podId': ?0, 'rolloverIssues.toSprintId': ?1 }")
  List<SprintPlanningDocument> findIncomingRollovers(String podId, Long toSprintId);
}
