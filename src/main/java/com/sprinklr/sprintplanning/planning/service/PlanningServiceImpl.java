package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.common.model.BacklogPage;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.common.util.StringListNormalizer;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculator;
import com.sprinklr.sprintplanning.planning.dto.BacklogPageDto;
import com.sprinklr.sprintplanning.planning.dto.IssueMoveRequest;
import com.sprinklr.sprintplanning.planning.dto.PlannedIssueViewDto;
import com.sprinklr.sprintplanning.planning.dto.PlannedScopeDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningIssuesPageDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningValidationResultDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningViewDto;
import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import com.sprinklr.sprintplanning.planning.model.PlanningOverride;
import com.sprinklr.sprintplanning.planning.model.RolloverIssue;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import com.sprinklr.sprintplanning.search.dto.TicketViewDto;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class PlanningServiceImpl implements PlanningService {

  private final TeamService teamService;
  private final JiraClient jiraClient;
  private final SprintPlanningDocumentAccessor planningDocumentAccessor;
  private final PlanningCalculator planningCalculator;
  private final PodJiraContextResolver podJiraContextResolver;
  private final PlanningIssueSelectionService issueSelectionService;
  private final PlanningViewAssembler viewAssembler;
  private final Executor jiraFetchExecutor;

  public PlanningServiceImpl(
      TeamService teamService,
      JiraClient jiraClient,
      SprintPlanningDocumentAccessor planningDocumentAccessor,
      PlanningCalculator planningCalculator,
      PodJiraContextResolver podJiraContextResolver,
      PlanningIssueSelectionService issueSelectionService,
      PlanningViewAssembler viewAssembler,
      Executor jiraFetchExecutor) {
    this.teamService = teamService;
    this.jiraClient = jiraClient;
    this.planningDocumentAccessor = planningDocumentAccessor;
    this.planningCalculator = planningCalculator;
    this.podJiraContextResolver = podJiraContextResolver;
    this.issueSelectionService = issueSelectionService;
    this.viewAssembler = viewAssembler;
    this.jiraFetchExecutor = jiraFetchExecutor;
  }

  @Override
  public SprintPlanningDocument getOrCreatePlanning(String podId, Long jiraSprintId) {
    return planningDocumentAccessor.getOrCreate(podId, jiraSprintId);
  }

  @Override
  public SprintPlanningDocument updateCapacity(String podId, Long jiraSprintId, List<PersonCapacity> capacity) {
    SprintPlanningDocument document = getOrCreatePlanning(podId, jiraSprintId);
    document.setCapacity(capacity != null ? capacity : List.of());
    return save(document);
  }

  @Override
  public SprintPlanningDocument updateLeaves(String podId, Long jiraSprintId, List<LeaveEntry> leaves) {
    SprintPlanningDocument document = getOrCreatePlanning(podId, jiraSprintId);
    document.setLeaves(leaves != null ? leaves : List.of());
    return save(document);
  }

  @Override
  public SprintPlanningDocument updateOverrides(String podId, Long jiraSprintId, List<PlanningOverride> overrides) {
    SprintPlanningDocument document = getOrCreatePlanning(podId, jiraSprintId);
    document.setOverrides(overrides != null ? overrides : List.of());
    return save(document);
  }

  @Override
  public SprintPlanningDocument updateCapacityAllocation(
      String podId,
      Long jiraSprintId,
      List<CapacityAllocationPercents> capacityAllocation) {
    SprintPlanningDocument document = getOrCreatePlanning(podId, jiraSprintId);
    document.setCapacityAllocation(capacityAllocation != null ? capacityAllocation : List.of());
    return save(document);
  }

  @Override
  public SprintPlanningDocument updateRolloverOverrides(
      String podId, Long jiraSprintId, Map<String, Double> rolloverStoryPoints) {
    SprintPlanningDocument document = getOrCreatePlanning(podId, jiraSprintId);
    document.setRolloverStoryPoints(rolloverStoryPoints != null ? rolloverStoryPoints : Map.of());
    return save(document);
  }

  @Override
  public PlanningSummaryDto calculateSummary(String podId, Long jiraSprintId) {
    PodJiraContext podContext = podJiraContextResolver.resolve(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    SprintView sprint = jiraClient.getSprint(jiraSprintId);
    List<IssueView> sprintIssues = jiraClient.getSprintIssues(jiraSprintId, podContext.fieldConfig());
    List<IssueView> selectedIssues = issueSelectionService.resolveSelectedIssues(
        planning, sprintIssues, podContext.boardId(), podContext.fieldConfig());
    Map<Domain, Double> computedRollover = fetchComputedRollover(podContext, planning, sprint);
    List<IssueView> committedIssues = resolveCommittedIssues(podId, planning);
    return viewAssembler.buildSummary(
        jiraSprintId, sprint, planning, computedRollover, selectedIssues, committedIssues);
  }

  @Override
  public PlanningValidationResultDto validate(String podId, Long jiraSprintId) {
    PlanningSummaryDto summary = calculateSummary(podId, jiraSprintId);
    return planningCalculator.validate(summary);
  }

  @Override
  public Map<Domain, Double> computeRollover(String podId, Long jiraSprintId) {
    PodJiraContext podContext = podJiraContextResolver.resolve(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    SprintView sprint = jiraClient.getSprint(jiraSprintId);
    return fetchComputedRollover(podContext, planning, sprint);
  }

  @Override
  public List<IssueView> resolveSelectedIssues(String podId, Long jiraSprintId) {
    PodJiraContext podContext = podJiraContextResolver.resolve(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    List<IssueView> sprintIssues = jiraClient.getSprintIssues(jiraSprintId, podContext.fieldConfig());
    return issueSelectionService.resolveSelectedIssues(
        planning, sprintIssues, podContext.boardId(), podContext.fieldConfig());
  }

  @Override
  public PlanningViewDto getPlanningView(String podId, Long jiraSprintId) {
    PodJiraContext podContext = podJiraContextResolver.resolve(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    List<String> committedKeys = nullSafeCopy(planning.getCommittedIssueKeys());

    CompletableFuture<SprintView> sprintFuture = CompletableFuture.supplyAsync(
        () -> jiraClient.getSprint(jiraSprintId), jiraFetchExecutor);
    CompletableFuture<List<IssueView>> sprintIssuesFuture = CompletableFuture.supplyAsync(
        () -> jiraClient.getSprintIssues(jiraSprintId, podContext.fieldConfig()), jiraFetchExecutor);
    CompletableFuture<List<SprintView>> closedSprintsFuture = CompletableFuture.supplyAsync(
        () -> listClosedSprints(podContext.boardId()), jiraFetchExecutor);
    CompletableFuture<List<IssueView>> committedIssuesFuture = committedKeys.isEmpty()
        ? CompletableFuture.completedFuture(List.of())
        : CompletableFuture.supplyAsync(
            () -> issueSelectionService.fetchCommittedIssues(podContext, committedKeys), jiraFetchExecutor);
    CompletableFuture<List<IssueView>> previousSprintIssuesFuture = sprintFuture
        .thenCombineAsync(
            closedSprintsFuture,
            (sprint, closedSprints) -> viewAssembler.findPreviousSprint(closedSprints, sprint),
            jiraFetchExecutor)
        .thenComposeAsync(
            previousSprint -> previousSprint == null
                ? CompletableFuture.completedFuture(List.of())
                : CompletableFuture.supplyAsync(
                    () -> jiraClient.getSprintIssues(previousSprint.id(), podContext.fieldConfig()),
                    jiraFetchExecutor),
            jiraFetchExecutor);

    SprintView sprint = PlanningAsyncSupport.joinFuture(sprintFuture);
    List<IssueView> sprintIssues = PlanningAsyncSupport.joinFuture(sprintIssuesFuture);
    List<SprintView> closedSprints = PlanningAsyncSupport.joinFuture(closedSprintsFuture);
    List<IssueView> committedIssues = PlanningAsyncSupport.joinFuture(committedIssuesFuture);
    List<IssueView> previousSprintIssues = PlanningAsyncSupport.joinFuture(previousSprintIssuesFuture);

    List<IssueView> selectedIssues = issueSelectionService.resolveSelectedIssues(
        planning, sprintIssues, podContext.boardId(), podContext.fieldConfig());
    SprintView previousSprint = viewAssembler.findPreviousSprint(closedSprints, sprint);
    Map<Domain, Double> resolvedRollover = previousSprint == null
        ? planningCalculator.resolveRollover(Map.of(), planning.getRolloverStoryPoints())
        : viewAssembler.resolveRolloverFromPreviousIssues(planning, previousSprintIssues);
    PlanningSummaryDto summary = viewAssembler.buildSummary(
        jiraSprintId, sprint, planning, resolvedRollover, selectedIssues, committedIssues);

    return viewAssembler.assemblePlanningView(
        jiraSprintId, sprint, planning, sprintIssues, resolvedRollover,
        selectedIssues, summary, committedKeys, podId);
  }

  @Override
  public PlanningIssuesPageDto getPlanningIssues(
      String podId, Long jiraSprintId, int startAt, int maxResults) {
    if (startAt < 0) {
      throw new ApiException("INVALID_PAGINATION", "startAt must be >= 0", HttpStatus.BAD_REQUEST);
    }
    if (maxResults < 1 || maxResults > 100) {
      throw new ApiException("INVALID_PAGINATION", "maxResults must be between 1 and 100", HttpStatus.BAD_REQUEST);
    }

    PodJiraContext podContext = podJiraContextResolver.resolve(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    List<IssueView> sprintIssues = jiraClient.getSprintIssues(jiraSprintId, podContext.fieldConfig());
    List<IssueView> selectedIssues = issueSelectionService.resolveSelectedIssues(
        planning, sprintIssues, podContext.boardId(), podContext.fieldConfig());

    int total = sprintIssues.size();
    int fromIndex = Math.min(startAt, total);
    int toIndex = Math.min(fromIndex + maxResults, total);
    List<IssueView> sprintIssuePage = sprintIssues.subList(fromIndex, toIndex);

    return new PlanningIssuesPageDto(
        sprintIssuePage,
        selectedIssues,
        startAt,
        maxResults,
        total,
        toIndex >= total);
  }

  @Override
  public PlannedScopeDto getPlannedScope(String podId, Long jiraSprintId) {
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    return toPlannedScopeDto(planning);
  }

  @Override
  public PlannedScopeDto updatePlannedScope(String podId, Long jiraSprintId, List<String> plannedIssueKeys) {
    teamService.getActivePodDocument(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    planning.setPlannedIssueKeys(StringListNormalizer.normalize(plannedIssueKeys));
    planning.setPlannedScopeCapturedAt(Instant.now());
    return toPlannedScopeDto(save(planning));
  }

  @Override
  public List<PlannedIssueViewDto> getPlannedIssues(String podId, Long jiraSprintId) {
    PodJiraContext podContext = podJiraContextResolver.resolve(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));

    List<String> plannedKeys = planning.getPlannedIssueKeys();
    if (plannedKeys.isEmpty()) {
      return List.of();
    }

    List<TicketViewDto> tickets = jiraClient.getIssuesByKeys(plannedKeys, podContext.fieldConfig());
    Map<String, TicketViewDto> ticketsByKey = tickets.stream()
        .collect(Collectors.toMap(TicketViewDto::key, ticket -> ticket, (left, right) -> left, LinkedHashMap::new));

    Set<String> committedKeys = new HashSet<>(nullSafeCopy(planning.getCommittedIssueKeys()));
    Set<String> outgoingRolloverKeys = planningDocumentAccessor.rolloverIssues(planning).stream()
        .map(RolloverIssue::getIssueKey)
        .collect(Collectors.toSet());

    List<PlannedIssueViewDto> plannedIssues = new ArrayList<>();
    for (String issueKey : plannedKeys) {
      TicketViewDto ticket = ticketsByKey.get(issueKey);
      if (ticket != null) {
        plannedIssues.add(toPlannedIssueView(ticket, jiraSprintId, committedKeys, outgoingRolloverKeys));
      }
    }
    return plannedIssues;
  }

  @Override
  public BacklogPageDto getBacklog(String podId, int startAt, int maxResults) {
    PodJiraContext podContext = podJiraContextResolver.resolve(podId);
    BacklogPage page = jiraClient.getBacklogIssues(
        podContext.boardId(), podContext.fieldConfig(), startAt, maxResults);
    return toBacklogPageDto(page);
  }

  @Override
  public PlanningViewDto moveIssuesToSprint(String podId, Long jiraSprintId, IssueMoveRequest request) {
    teamService.getActivePodDocument(podId);
    List<String> issueKeys = StringListNormalizer.normalize(request.issueKeys());

    jiraClient.moveIssuesToSprint(issueKeys, jiraSprintId);

    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    planning.setCommittedIssueKeys(mergeIssueKeys(planning.getCommittedIssueKeys(), issueKeys));
    planning.setCommittedAt(Instant.now());
    if (request.shouldAddToPlannedScope()) {
      planning.setPlannedIssueKeys(mergeIssueKeys(planning.getPlannedIssueKeys(), issueKeys));
      planning.setPlannedScopeCapturedAt(Instant.now());
    }
    save(planning);

    return getPlanningView(podId, jiraSprintId);
  }

  @Override
  public BacklogPageDto moveIssuesToBacklog(
      String podId, Long jiraSprintId, int startAt, int maxResults, List<String> issueKeys) {
    teamService.getActivePodDocument(podId);
    List<String> normalizedKeys = StringListNormalizer.normalize(issueKeys);
    jiraClient.moveIssuesToBacklog(normalizedKeys);
    if (jiraSprintId != null && !normalizedKeys.isEmpty()) {
      removeCommittedIssueKeys(podId, jiraSprintId, normalizedKeys);
    }
    return getBacklog(podId, startAt, maxResults);
  }

  @Override
  public PlanningViewDto uncommitIssues(String podId, Long jiraSprintId, List<String> issueKeys) {
    teamService.getActivePodDocument(podId);
    List<String> normalizedKeys = StringListNormalizer.normalize(issueKeys);
    if (!normalizedKeys.isEmpty()) {
      removeCommittedIssueKeys(podId, jiraSprintId, normalizedKeys);
    }
    return getPlanningView(podId, jiraSprintId);
  }

  private Map<Domain, Double> fetchComputedRollover(
      PodJiraContext podContext,
      SprintPlanningDocument planning,
      SprintView currentSprint) {
    return fetchComputedRollover(
        podContext, planning, currentSprint, listClosedSprints(podContext.boardId()));
  }

  private Map<Domain, Double> fetchComputedRollover(
      PodJiraContext podContext,
      SprintPlanningDocument planning,
      SprintView currentSprint,
      List<SprintView> closedSprints) {
    SprintView previousSprint = viewAssembler.findPreviousSprint(closedSprints, currentSprint);
    if (previousSprint == null) {
      return planningCalculator.resolveRollover(Map.of(), planning.getRolloverStoryPoints());
    }

    List<IssueView> previousIssues =
        jiraClient.getSprintIssues(previousSprint.id(), podContext.fieldConfig());
    return viewAssembler.resolveRolloverFromPreviousIssues(planning, previousIssues);
  }

  private List<IssueView> resolveCommittedIssues(String podId, SprintPlanningDocument planning) {
    List<String> committedKeys = planning.getCommittedIssueKeys();
    if (committedKeys == null || committedKeys.isEmpty()) {
      return List.of();
    }
    PodJiraContext podContext = podJiraContextResolver.resolve(podId);
    return issueSelectionService.fetchCommittedIssues(podContext, committedKeys);
  }

  private List<SprintView> listClosedSprints(Long boardId) {
    return jiraClient.getBoardSprints(boardId, "closed").stream()
        .sorted(Comparator.comparing(SprintView::endDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private void removeCommittedIssueKeys(String podId, Long jiraSprintId, List<String> issueKeys) {
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    List<String> updatedKeys = removeIssueKeys(planning.getCommittedIssueKeys(), issueKeys);
    if (updatedKeys.size() == planning.getCommittedIssueKeys().size()) {
      return;
    }
    planning.setCommittedIssueKeys(updatedKeys);
    planning.setCommittedAt(Instant.now());
    save(planning);
  }

  private PlannedScopeDto toPlannedScopeDto(SprintPlanningDocument planning) {
    return new PlannedScopeDto(
        nullSafeCopy(planning.getPlannedIssueKeys()),
        planning.getPlannedScopeCapturedAt());
  }

  private PlannedIssueViewDto toPlannedIssueView(
      TicketViewDto ticket,
      Long plannedSprintId,
      Set<String> committedKeys,
      Set<String> outgoingRolloverKeys) {
    Long currentSprintId = ticket.currentSprintId();
    boolean onPlan = plannedSprintId.equals(currentSprintId);
    boolean rolledOver = outgoingRolloverKeys.contains(ticket.key())
        || (currentSprintId != null && !onPlan)
        || (currentSprintId == null && committedKeys.contains(ticket.key()));

    return new PlannedIssueViewDto(
        ticket.key(),
        ticket.summary(),
        ticket.issueType(),
        ticket.status(),
        ticket.statusCategory(),
        ticket.storyPoints(),
        ticket.domain(),
        ticket.domainLabel(),
        plannedSprintId,
        currentSprintId,
        rolledOver);
  }

  private List<String> mergeIssueKeys(List<String> existingKeys, List<String> newKeys) {
    LinkedHashSet<String> merged = new LinkedHashSet<>();
    if (existingKeys != null) {
      merged.addAll(existingKeys);
    }
    merged.addAll(newKeys);
    return new ArrayList<>(merged);
  }

  private List<String> removeIssueKeys(List<String> existingKeys, List<String> keysToRemove) {
    if (existingKeys == null || existingKeys.isEmpty()) {
      return List.of();
    }
    Set<String> removeSet = new HashSet<>(keysToRemove);
    return existingKeys.stream()
        .filter(key -> !removeSet.contains(key))
        .toList();
  }

  private BacklogPageDto toBacklogPageDto(BacklogPage page) {
    return new BacklogPageDto(
        page.issues(),
        page.startAt(),
        page.maxResults(),
        page.total(),
        page.last());
  }

  private SprintPlanningDocument save(SprintPlanningDocument document) {
    return planningDocumentAccessor.save(document);
  }

  private List<String> nullSafeCopy(List<String> values) {
    return values != null ? List.copyOf(values) : List.of();
  }
}
