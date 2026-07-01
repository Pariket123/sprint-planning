package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.planning.dto.CapacityAllocationRowDto;
import com.sprinklr.sprintplanning.planning.dto.CapacityAllocationTableDto;
import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CapacityAllocationCalculator {

  public static final String TOTAL_KEY = "TOTAL";
  public static final double DEFAULT_ROADMAP_PERCENT = 60.0;
  public static final double DEFAULT_BUG_SUPPORT_PERCENT = 40.0;
  public static final List<Domain> ENGINEERING_DOMAINS = List.of(
      Domain.BE, Domain.UI, Domain.AI, Domain.QA);

  public CapacityAllocationTableDto buildTable(
      Map<Domain, Double> availableByDomain,
      List<CapacityAllocationPercents> savedPercents) {
    Map<String, CapacityAllocationPercents> savedByKey = indexSaved(savedPercents);
    List<CapacityAllocationRowDto> rows = new ArrayList<>();

    double totalAvailable = 0;
    for (Domain domain : ENGINEERING_DOMAINS) {
      double available = round(Math.max(0, availableByDomain.getOrDefault(domain, 0.0)));
      totalAvailable += available;
      rows.add(buildRow(domain.name(), labelFor(domain), available, savedByKey.get(domain.name())));
    }

    rows.addFirst(buildRow(
        TOTAL_KEY,
        "Total",
        round(totalAvailable),
        savedByKey.get(TOTAL_KEY)));

    return new CapacityAllocationTableDto(rows);
  }

  public double plannedRoadmapStoryPoints(CapacityAllocationTableDto table, Domain domain) {
    if (table == null || table.rows() == null) {
      return 0.0;
    }
    String key = domain == null ? TOTAL_KEY : domain.name();
    return table.rows().stream()
        .filter(row -> row.key().equalsIgnoreCase(key))
        .map(CapacityAllocationRowDto::plannedRoadmapStoryPoints)
        .findFirst()
        .orElse(0.0);
  }

  public Map<Domain, Double> engineeringAvailable(Map<Domain, Double> availableByDomain) {
    Map<Domain, Double> engineering = new LinkedHashMap<>();
    for (Domain domain : ENGINEERING_DOMAINS) {
      engineering.put(domain, Math.max(0, availableByDomain.getOrDefault(domain, 0.0)));
    }
    return engineering;
  }

  private CapacityAllocationRowDto buildRow(
      String key,
      String label,
      double available,
      CapacityAllocationPercents saved) {
    double roadmapPercent = saved != null ? saved.getRoadmapPercent() : DEFAULT_ROADMAP_PERCENT;
    double bugSupportPercent = saved != null ? saved.getBugSupportPercent() : DEFAULT_BUG_SUPPORT_PERCENT;
    return new CapacityAllocationRowDto(
        key,
        label,
        available,
        round(roadmapPercent),
        round(bugSupportPercent),
        round(available * roadmapPercent / 100.0),
        round(available * bugSupportPercent / 100.0));
  }

  private Map<String, CapacityAllocationPercents> indexSaved(List<CapacityAllocationPercents> savedPercents) {
    Map<String, CapacityAllocationPercents> indexed = new LinkedHashMap<>();
    if (savedPercents == null) {
      return indexed;
    }
    for (CapacityAllocationPercents entry : savedPercents) {
      if (entry != null && entry.getKey() != null && !entry.getKey().isBlank()) {
        indexed.put(entry.getKey().trim().toUpperCase(), entry);
      }
    }
    return indexed;
  }

  private String labelFor(Domain domain) {
    return switch (domain) {
      case BE -> "Backend";
      case UI -> "UI";
      case AI -> "AI";
      case QA -> "QA";
      default -> domain.name();
    };
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
