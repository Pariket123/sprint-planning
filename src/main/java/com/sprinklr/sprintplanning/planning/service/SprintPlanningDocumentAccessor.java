package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import com.sprinklr.sprintplanning.planning.model.PlanningOverride;
import com.sprinklr.sprintplanning.planning.model.RolloverIssue;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import com.sprinklr.sprintplanning.planning.repository.SprintPlanningRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SprintPlanningDocumentAccessor {

  private final SprintPlanningRepository sprintPlanningRepository;

  public SprintPlanningDocumentAccessor(SprintPlanningRepository sprintPlanningRepository) {
    this.sprintPlanningRepository = sprintPlanningRepository;
  }

  public SprintPlanningDocument getOrCreate(String podId, Long jiraSprintId) {
    List<SprintPlanningDocument> documents =
        sprintPlanningRepository.findAllByPodIdAndJiraSprintIdOrderByUpdatedAtDesc(podId, jiraSprintId);

    if (documents.isEmpty()) {
      return createPlanning(podId, jiraSprintId);
    }

    SprintPlanningDocument primary = normalize(documents.getFirst());
    for (int index = 1; index < documents.size(); index++) {
      sprintPlanningRepository.delete(documents.get(index));
    }

    return documents.size() > 1 ? save(primary) : primary;
  }

  public SprintPlanningDocument save(SprintPlanningDocument document) {
    normalize(document);
    document.setUpdatedAt(Instant.now());
    return sprintPlanningRepository.save(document);
  }

  public SprintPlanningDocument normalize(SprintPlanningDocument document) {
    if (document.getCapacity() == null) {
      document.setCapacity(new ArrayList<>());
    } else {
      document.setCapacity(migrateLegacyCapacity(document.getCapacity()));
    }
    if (document.getLeaves() == null) {
      document.setLeaves(new ArrayList<>());
    }
    if (document.getOverrides() == null) {
      document.setOverrides(new ArrayList<>());
    }
    if (document.getRolloverStoryPoints() == null) {
      document.setRolloverStoryPoints(new HashMap<>());
    }
    if (document.getPlannedIssueKeys() == null) {
      document.setPlannedIssueKeys(new ArrayList<>());
    }
    if (document.getCommittedIssueKeys() == null) {
      document.setCommittedIssueKeys(new ArrayList<>());
    }
    if (document.getRolloverIssues() == null) {
      document.setRolloverIssues(new ArrayList<>());
    }
    return document;
  }

  private List<PersonCapacity> migrateLegacyCapacity(List<PersonCapacity> capacity) {
    List<PersonCapacity> migrated = new ArrayList<>();
    for (PersonCapacity entry : capacity) {
      if (entry == null) {
        continue;
      }
      if (entry.getPersonName() != null && !entry.getPersonName().isBlank()) {
        entry.setHeadcount(null);
        migrated.add(entry);
        continue;
      }
      Integer headcount = entry.getHeadcount();
      if (headcount == null || headcount <= 0) {
        headcount = 1;
      }
      if (entry.getDomain() == null) {
        continue;
      }
      for (int index = 1; index <= headcount; index++) {
        PersonCapacity person = new PersonCapacity();
        person.setPersonName(entry.getDomain().name() + " " + index);
        person.setDomain(entry.getDomain());
        person.setBandwidthPercent(entry.getBandwidthPercent());
        migrated.add(person);
      }
    }
    return migrated;
  }

  public List<PlanningOverride> overrides(SprintPlanningDocument document) {
    return normalize(document).getOverrides();
  }

  public List<RolloverIssue> rolloverIssues(SprintPlanningDocument document) {
    return normalize(document).getRolloverIssues();
  }

  public Map<String, Double> rolloverStoryPoints(SprintPlanningDocument document) {
    return normalize(document).getRolloverStoryPoints();
  }

  private SprintPlanningDocument createPlanning(String podId, Long jiraSprintId) {
    SprintPlanningDocument document = new SprintPlanningDocument();
    document.setPodId(podId);
    document.setJiraSprintId(jiraSprintId);
    document.setCreatedAt(Instant.now());
    document.setUpdatedAt(Instant.now());
    normalize(document);
    try {
      return sprintPlanningRepository.save(document);
    } catch (DuplicateKeyException ex) {
      return getOrCreate(podId, jiraSprintId);
    }
  }
}
