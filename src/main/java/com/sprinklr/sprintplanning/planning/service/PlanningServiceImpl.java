package com.sprinklr.sprintplanning.planning.service;

import com.sprinklr.sprintplanning.client.jira.JiraClient;
import com.sprinklr.sprintplanning.common.enums.Domain;
import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.common.model.BacklogPage;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.SprintView;
import com.sprinklr.sprintplanning.common.util.StringListNormalizer;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculationInput;
import com.sprinklr.sprintplanning.planning.calculator.PlanningCalculator;
import com.sprinklr.sprintplanning.planning.dto.BacklogPageDto;
import com.sprinklr.sprintplanning.planning.dto.IssueMoveRequest;
import com.sprinklr.sprintplanning.planning.dto.PlannedIssueViewDto;
import com.sprinklr.sprintplanning.planning.dto.PlannedScopeDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningIssuesPageDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningSummaryDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningValidationResultDto;
import com.sprinklr.sprintplanning.planning.dto.PlanningViewDto;
import com.sprinklr.sprintplanning.planning.mapper.PlanningMapper;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import com.sprinklr.sprintplanning.planning.model.LeaveEntry;
import com.sprinklr.sprintplanning.planning.model.OverrideAction;
import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import com.sprinklr.sprintplanning.planning.model.PlanningOverride;
import com.sprinklr.sprintplanning.planning.model.RolloverIssue;
import com.sprinklr.sprintplanning.planning.model.SprintPlanningDocument;
import com.sprinklr.sprintplanning.planning.repository.SprintPlanningRepository;
import com.sprinklr.sprintplanning.search.dto.TicketViewDto;
import com.sprinklr.sprintplanning.team.mapper.JiraConfigMapper;
import com.sprinklr.sprintplanning.team.model.PodDocument;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class PlanningServiceImpl implements PlanningService {

  private final TeamService teamService;
  private final JiraClient jiraClient;
  private final JiraConfigMapper jiraConfigMapper;
  private final SprintPlanningRepository sprintPlanningRepository;
  private final SprintPlanningDocumentAccessor planningDocumentAccessor;
  private final PlanningCalculator planningCalculator;
  private final PlanningMapper planningMapper;
  private final RolloverService rolloverService;
  private final Executor jiraFetchExecutor;

  public PlanningServiceImpl(
      TeamService teamService,
      JiraClient jiraClient,
      JiraConfigMapper jiraConfigMapper,
      SprintPlanningRepository sprintPlanningRepository,
      SprintPlanningDocumentAccessor planningDocumentAccessor,
      PlanningCalculator planningCalculator,
      PlanningMapper planningMapper,
      RolloverService rolloverService,
      Executor jiraFetchExecutor) {
    this.teamService = teamService;
    this.jiraClient = jiraClient;
    this.jiraConfigMapper = jiraConfigMapper;
    this.sprintPlanningRepository = sprintPlanningRepository;
    this.planningDocumentAccessor = planningDocumentAccessor;
    this.planningCalculator = planningCalculator;
    this.planningMapper = planningMapper;
    this.rolloverService = rolloverService;
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
    PodContext podContext = resolvePodContext(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    SprintView sprint = jiraClient.getSprint(jiraSprintId);
    List<IssueView> sprintIssues = jiraClient.getSprintIssues(jiraSprintId, podContext.fieldConfig());
    List<IssueView> selectedIssues = resolveSelectedIssues(
        planning, sprintIssues, podContext.boardId(), podContext.fieldConfig());
    Map<Domain, Double> computedRollover = fetchComputedRollover(
        podContext, planning, sprint);
    List<IssueView> committedIssues = resolveCommittedIssues(podId, planning);
    return buildSummary(jiraSprintId, sprint, planning, computedRollover, selectedIssues, committedIssues);
  }

  @Override
  public PlanningValidationResultDto validate(String podId, Long jiraSprintId) {
    PlanningSummaryDto summary = calculateSummary(podId, jiraSprintId);
    return planningCalculator.validate(summary);
  }

  @Override
  public Map<Domain, Double> computeRollover(String podId, Long jiraSprintId) {
    PodContext podContext = resolvePodContext(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    SprintView sprint = jiraClient.getSprint(jiraSprintId);
    return fetchComputedRollover(podContext, planning, sprint);
  }

  private Map<Domain, Double> fetchComputedRollover(
      PodContext podContext,
      SprintPlanningDocument planning,
      SprintView currentSprint) {
    return fetchComputedRollover(
        podContext, planning, currentSprint, listClosedSprints(podContext.boardId()));
  }

  private Map<Domain, Double> fetchComputedRollover(
      PodContext podContext,
      SprintPlanningDocument planning,
      SprintView currentSprint,
      List<SprintView> closedSprints) {
    SprintView previousSprint = findPreviousSprint(closedSprints, currentSprint);
    if (previousSprint == null) {
      return planningCalculator.resolveRollover(Map.of(), planning.getRolloverStoryPoints());
    }

    List<IssueView> previousIssues =
        jiraClient.getSprintIssues(previousSprint.id(), podContext.fieldConfig());
    return computeRolloverFromPreviousIssues(planning, previousIssues);
  }

  private Map<Domain, Double> computeRolloverFromPreviousIssues(
      SprintPlanningDocument planning,
      List<IssueView> previousSprintIssues) {
    Map<Domain, Double> computed = planningCalculator.computeRolloverFromIssues(previousSprintIssues);
    return planningCalculator.resolveRollover(computed, planning.getRolloverStoryPoints());
  }

  private List<SprintView> listClosedSprints(Long boardId) {
    return jiraClient.getBoardSprints(boardId, "closed").stream()
        .sorted(Comparator.comparing(SprintView::endDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  @Override
  public List<IssueView> resolveSelectedIssues(String podId, Long jiraSprintId) {
    PodContext podContext = resolvePodContext(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    List<IssueView> sprintIssues = jiraClient.getSprintIssues(jiraSprintId, podContext.fieldConfig());
    return resolveSelectedIssues(planning, sprintIssues, podContext.boardId(), podContext.fieldConfig());
  }

  private List<IssueView> resolveSelectedIssues(
      SprintPlanningDocument planning,
      List<IssueView> sprintIssues,
      Long boardId,
      JiraFieldConfig fieldConfig) {
    Set<String> excludeKeys = planningDocumentAccessor.overrides(planning).stream()
        .filter(override -> override.getAction() == OverrideAction.EXCLUDE)
        .map(PlanningOverride::getIssueKey)
        .collect(Collectors.toSet());

    Set<String> includeKeys = planningDocumentAccessor.overrides(planning).stream()
        .filter(override -> override.getAction() == OverrideAction.INCLUDE)
        .map(PlanningOverride::getIssueKey)
        .collect(Collectors.toSet());

    Map<String, IssueView> selected = new LinkedHashMap<>();
    for (IssueView issue : sprintIssues) {
      if (!excludeKeys.contains(issue.key())) {
        selected.put(issue.key(), issue);
      }
    }

    if (!includeKeys.isEmpty()) {
      Set<String> missingIncludeKeys = new HashSet<>(includeKeys);
      missingIncludeKeys.removeAll(selected.keySet());
      if (!missingIncludeKeys.isEmpty()) {
        List<IssueView> backlogIssues = jiraClient.getBacklogIssues(boardId, fieldConfig);
        for (IssueView issue : backlogIssues) {
          if (missingIncludeKeys.contains(issue.key())) {
            selected.put(issue.key(), issue);
            missingIncludeKeys.remove(issue.key());
          }
        }
      }
    }

    return new ArrayList<>(selected.values());
  }

  private List<IssueView> resolveCommittedIssues(String podId, SprintPlanningDocument planning) {
    List<String> committedKeys = planning.getCommittedIssueKeys();
    if (committedKeys == null || committedKeys.isEmpty()) {
      return List.of();
    }

    PodContext podContext = resolvePodContext(podId);
    return fetchCommittedIssues(podContext, committedKeys);
  }

  private List<IssueView> fetchCommittedIssues(PodContext podContext, List<String> committedKeys) {
    if (committedKeys == null || committedKeys.isEmpty()) {
      return List.of();
    }

    List<TicketViewDto> tickets = jiraClient.getIssuesByKeys(committedKeys, podContext.fieldConfig());
    return tickets.stream()
        .map(this::toIssueView)
        .toList();
  }

  private IssueView toIssueView(TicketViewDto ticket) {
    return new IssueView(
        ticket.key(),
        ticket.summary(),
        ticket.domain(),
        ticket.storyPoints(),
        ticket.issueType(),
        ticket.status(),
        ticket.statusCategory(),
        ticket.domainAllocations() != null ? ticket.domainAllocations() : List.of());
  }

  @Override
  public PlanningViewDto getPlanningView(String podId, Long jiraSprintId) {
    PodContext podContext = resolvePodContext(podId);
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
            () -> fetchCommittedIssues(podContext, committedKeys), jiraFetchExecutor);

    SprintView sprint = joinFuture(sprintFuture);
    List<IssueView> sprintIssues = joinFuture(sprintIssuesFuture);
    List<SprintView> closedSprints = joinFuture(closedSprintsFuture);
    List<IssueView> committedIssues = joinFuture(committedIssuesFuture);

    List<IssueView> selectedIssues = resolveSelectedIssues(
        planning, sprintIssues, podContext.boardId(), podContext.fieldConfig());
    Map<Domain, Double> resolvedRollover = fetchComputedRollover(
        podContext, planning, sprint, closedSprints);
    PlanningSummaryDto summary = buildSummary(
        jiraSprintId, sprint, planning, resolvedRollover, selectedIssues, committedIssues);

    return new PlanningViewDto(
        jiraSprintId,
        sprint,
        planning.getCapacity(),
        planning.getLeaves(),
        planning.getOverrides(),
        planning.getCapacityAllocation(),
        planning.getRolloverStoryPoints(),
        planningMapper.toResolvedRolloverMap(resolvedRollover),
        sprintIssues.size(),
        selectedIssues.size(),
        selectedIssues.stream().map(IssueView::key).toList(),
        nullSafeCopy(planning.getPlannedIssueKeys()),
        committedKeys,
        rolloverService.getRolloverRecords(podId, jiraSprintId),
        summary.domainMetrics(),
        summary.capacityAllocationTable());
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

    PodContext podContext = resolvePodContext(podId);
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));
    List<IssueView> sprintIssues = jiraClient.getSprintIssues(jiraSprintId, podContext.fieldConfig());
    List<IssueView> selectedIssues = resolveSelectedIssues(
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

  private static <T> T joinFuture(CompletableFuture<T> future) {
    try {
      return future.join();
    } catch (CompletionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw ex;
    }
  }

  private PlanningSummaryDto buildSummary(
      Long jiraSprintId,
      SprintView sprint,
      SprintPlanningDocument planning,
      Map<Domain, Double> computedRollover,
      List<IssueView> selectedIssues,
      List<IssueView> committedIssues) {
    PlanningCalculationInput input = new PlanningCalculationInput(
        jiraSprintId,
        sprint.startDate(),
        sprint.endDate(),
        planning.getCapacity(),
        planning.getLeaves(),
        planning.getRolloverStoryPoints(),
        computedRollover,
        selectedIssues,
        committedIssues,
        planning.getCapacityAllocation());
    return planningCalculator.calculateSummary(input);
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
    PodDocument pod = teamService.getActivePodDocument(podId);
    JiraFieldConfig fieldConfig = jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig());
    SprintPlanningDocument planning = planningDocumentAccessor.normalize(getOrCreatePlanning(podId, jiraSprintId));

    List<String> plannedKeys = planning.getPlannedIssueKeys();
    if (plannedKeys.isEmpty()) {
      return List.of();
    }

    List<TicketViewDto> tickets = jiraClient.getIssuesByKeys(plannedKeys, fieldConfig);
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
    PodContext podContext = resolvePodContext(podId);
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
      String podId, int startAt, int maxResults, List<String> issueKeys) {
    teamService.getActivePodDocument(podId);
    jiraClient.moveIssuesToBacklog(issueKeys);
    return getBacklog(podId, startAt, maxResults);
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
    Long currentSprintId = resolveCurrentSprintId(ticket.sprintIds());
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
        plannedSprintId,
        currentSprintId,
        rolledOver);
  }

  private Long resolveCurrentSprintId(List<Long> sprintIds) {
    if (sprintIds == null || sprintIds.isEmpty()) {
      return null;
    }
    return sprintIds.get(sprintIds.size() - 1);
  }

  private List<String> mergeIssueKeys(List<String> existingKeys, List<String> newKeys) {
    LinkedHashSet<String> merged = new LinkedHashSet<>();
    if (existingKeys != null) {
      merged.addAll(existingKeys);
    }
    merged.addAll(newKeys);
    return new ArrayList<>(merged);
  }

  private BacklogPageDto toBacklogPageDto(BacklogPage page) {
    return new BacklogPageDto(
        page.issues(),
        page.startAt(),
        page.maxResults(),
        page.total(),
        page.last());
  }

  private PodContext resolvePodContext(String podId) {
    PodDocument pod = teamService.getActivePodDocument(podId);
    return new PodContext(
        requireBoardId(pod),
        jiraConfigMapper.toJiraFieldConfig(pod.getJiraConfig()));
  }

  private record PodContext(Long boardId, JiraFieldConfig fieldConfig) {
  }

  private SprintView findPreviousSprint(List<SprintView> closedSprints, SprintView currentSprint) {
    Instant currentStart = currentSprint.startDate();

    return closedSprints.stream()
        .filter(sprint -> !sprint.id().equals(currentSprint.id()))
        .filter(sprint -> sprint.endDate() != null
            && (currentStart == null || !sprint.endDate().isAfter(currentStart)))
        .max(Comparator.comparing(SprintView::endDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElse(null);
  }

  private SprintPlanningDocument save(SprintPlanningDocument document) {
    return planningDocumentAccessor.save(document);
  }

  private List<String> nullSafeCopy(List<String> values) {
    return values != null ? List.copyOf(values) : List.of();
  }

  private Long requireBoardId(PodDocument pod) {
    Long boardId = pod.getJiraConfig() != null ? pod.getJiraConfig().getBoardId() : null;
    if (boardId == null) {
      throw new ApiException(
          "POD_JIRA_NOT_CONFIGURED",
          "Pod does not have a Jira board configured: " + pod.getId(),
          HttpStatus.BAD_REQUEST);
    }
    return boardId;
  }
}

