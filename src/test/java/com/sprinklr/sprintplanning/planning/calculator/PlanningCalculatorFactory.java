package com.sprinklr.sprintplanning.planning.calculator;

import com.sprinklr.sprintplanning.planning.config.PlanningProperties;

public final class PlanningCalculatorFactory {

  private PlanningCalculatorFactory() {
  }

  public static PlanningCalculator create(PlanningProperties properties) {
    CapacityAllocationCalculator capacityAllocationCalculator = new CapacityAllocationCalculator();
    CapacityCalculator capacityCalculator = new CapacityCalculator();
    RolloverCalculator rolloverCalculator = new RolloverCalculator();
    PlanningValidator planningValidator = new PlanningValidator(properties);
    PlanningSummaryAssembler summaryAssembler = new PlanningSummaryAssembler(
        capacityCalculator, rolloverCalculator, capacityAllocationCalculator, properties);
    return new PlanningCalculator(
        capacityCalculator, rolloverCalculator, planningValidator, summaryAssembler);
  }
}
