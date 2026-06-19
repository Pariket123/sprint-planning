package com.sprinklr.sprintplanning.team.repository;

import com.sprinklr.sprintplanning.team.model.TeamDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends MongoRepository<TeamDocument, String> {

  List<TeamDocument> findByActiveTrueOrderByNameAsc();

  Optional<TeamDocument> findByCode(String code);
}
