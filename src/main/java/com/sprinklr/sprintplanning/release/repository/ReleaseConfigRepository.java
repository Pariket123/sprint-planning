package com.sprinklr.sprintplanning.release.repository;

import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReleaseConfigRepository extends MongoRepository<ReleaseConfigDocument, String> {

  List<ReleaseConfigDocument> findByTeamIdAndActiveTrueOrderByNameAsc(String teamId);

  Optional<ReleaseConfigDocument> findByIdAndTeamIdAndActiveTrue(String id, String teamId);
}
