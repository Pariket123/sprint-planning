package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CapacityAllocationCalculatorTest {

  private final CapacityAllocationCalculator calculator = new CapacityAllocationCalculator();

  @Test
  void buildsTableWithDefaultPercentsAndPlannedStoryPoints() {
    var table = calculator.buildTable(
        Map.of(Domain.BE, 10.0, Domain.UI, 5.0),
        List.of());

    assertThat(table.rows()).hasSize(5);
    assertThat(table.rows().getFirst().key()).isEqualTo("TOTAL");
    assertThat(table.rows().getFirst().availableStoryPoints()).isEqualTo(15.0);
    assertThat(table.rows().getFirst().plannedRoadmapStoryPoints()).isEqualTo(9.0);
    assertThat(table.rows().getFirst().plannedBugSupportStoryPoints()).isEqualTo(6.0);

    assertThat(table.rows()).filteredOn(row -> row.key().equals("BE")).first()
        .satisfies(be -> {
          assertThat(be.roadmapPercent()).isEqualTo(60.0);
          assertThat(be.bugSupportPercent()).isEqualTo(40.0);
          assertThat(be.plannedRoadmapStoryPoints()).isEqualTo(6.0);
          assertThat(be.plannedBugSupportStoryPoints()).isEqualTo(4.0);
        });
  }

  @Test
  void usesSavedPercentsWhenProvided() {
    CapacityAllocationPercents bePercents = new CapacityAllocationPercents();
    bePercents.setKey("BE");
    bePercents.setRoadmapPercent(70.0);
    bePercents.setBugSupportPercent(30.0);

    var table = calculator.buildTable(
        Map.of(Domain.BE, 10.0),
        List.of(bePercents));

    assertThat(table.rows()).filteredOn(row -> row.key().equals("BE")).first()
        .satisfies(be -> {
          assertThat(be.roadmapPercent()).isEqualTo(70.0);
          assertThat(be.plannedRoadmapStoryPoints()).isEqualTo(7.0);
          assertThat(be.plannedBugSupportStoryPoints()).isEqualTo(3.0);
        });
  }

  @Test
  void resolvesPlannedRoadmapStoryPointsByDomain() {
    var table = calculator.buildTable(Map.of(Domain.BE, 10.0), List.of());

    assertThat(calculator.plannedRoadmapStoryPoints(table, Domain.BE)).isEqualTo(6.0);
    assertThat(calculator.plannedRoadmapStoryPoints(table, null)).isEqualTo(6.0);
  }
}
