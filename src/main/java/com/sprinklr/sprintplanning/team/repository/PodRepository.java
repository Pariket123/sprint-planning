package com.sprinklr.sprintplanning.team.repository;

import com.sprinklr.sprintplanning.team.model.PodDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PodRepository extends MongoRepository<PodDocument, String> {

  List<PodDocument> findByTeamIdAndActiveTrueOrderByNameAsc(String teamId);

  Optional<PodDocument> findByIdAndActiveTrue(String id);
}
