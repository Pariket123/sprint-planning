package com.sprinklr.sprintplanning.search;

import com.sprinklr.sprintplanning.common.util.StringListNormalizer;
import com.sprinklr.sprintplanning.release.model.ReleaseBasicFilters;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class FilterMergeHelper {

  public MergeResult mergeWithRelease(ReleaseConfigDocument release, IssueSearchFilters requestFilters) {
    IssueSearchFilters request = normalize(requestFilters != null ? requestFilters : IssueSearchFilters.empty());

    List<String> fixVersionIncludes = intersectOrLeft(
        StringListNormalizer.normalize(release.getFixVersionIncludes()),
        request.fixVersions());
    if (fixVersionIncludes != null && fixVersionIncludes.isEmpty()) {
      return MergeResult.conflicting();
    }

    List<String> fixVersionExcludes = union(
        StringListNormalizer.normalize(release.getFixVersionExcludes()),
        request.fixVersionExcludes());

    ReleaseBasicFilters basic = release.getBasicFilters() != null
        ? release.getBasicFilters()
        : new ReleaseBasicFilters();

    List<String> issueTypes = intersectOrLeft(
        StringListNormalizer.normalize(basic.getIssueTypes()), request.issueTypes());
    if (issueTypes != null && issueTypes.isEmpty()) {
      return MergeResult.conflicting();
    }

    List<String> statuses = intersectOrLeft(
        StringListNormalizer.normalize(basic.getStatuses()), request.statuses());
    if (statuses != null && statuses.isEmpty()) {
      return MergeResult.conflicting();
    }

    List<String> domains = intersectOrLeft(
        StringListNormalizer.normalize(basic.getDomains()), request.domains());
    if (domains != null && domains.isEmpty()) {
      return MergeResult.conflicting();
    }

    List<String> priorities = intersectOrLeft(
        StringListNormalizer.normalize(basic.getPriorities()), request.priorities());
    if (priorities != null && priorities.isEmpty()) {
      return MergeResult.conflicting();
    }

    List<String> assigneeIds = intersectOrLeft(
        StringListNormalizer.normalize(basic.getAssigneeIds()), request.assigneeIds());
    if (assigneeIds != null && assigneeIds.isEmpty()) {
      return MergeResult.conflicting();
    }

    IssueSearchFilters merged = new IssueSearchFilters(
        issueTypes,
        statuses,
        domains,
        request.sprintIds(),
        fixVersionIncludes,
        fixVersionExcludes.isEmpty() ? null : fixVersionExcludes,
        assigneeIds,
        priorities,
        request.issueKeys(),
        null,
        request.labels(),
        request.components());

    return new MergeResult(merged);
  }

  public IssueSearchFilters normalize(IssueSearchFilters filters) {
    if (filters == null) {
      return IssueSearchFilters.empty();
    }
    return new IssueSearchFilters(
        normalizeOrNull(filters.issueTypes()),
        normalizeOrNull(filters.statuses()),
        normalizeOrNull(filters.domains()),
        filters.sprintIds(),
        normalizeOrNull(filters.fixVersions()),
        normalizeOrNull(filters.fixVersionExcludes()),
        normalizeOrNull(filters.assigneeIds()),
        normalizeOrNull(filters.priorities()),
        normalizeOrNull(filters.issueKeys()),
        normalizeOrNull(filters.podIds()),
        normalizeOrNull(filters.labels()),
        normalizeOrNull(filters.components()));
  }

  private List<String> normalizeOrNull(List<String> values) {
    List<String> normalized = StringListNormalizer.normalize(values);
    return normalized.isEmpty() ? null : normalized;
  }

  private List<String> intersectOrLeft(List<String> releaseValues, List<String> requestValues) {
    if (requestValues == null || requestValues.isEmpty()) {
      return releaseValues.isEmpty() ? null : releaseValues;
    }
    if (releaseValues.isEmpty()) {
      return requestValues;
    }
    Set<String> intersection = new LinkedHashSet<>(releaseValues);
    intersection.retainAll(requestValues);
    return new ArrayList<>(intersection);
  }

  private List<String> union(List<String> left, List<String> right) {
    Set<String> merged = new LinkedHashSet<>();
    if (left != null) {
      merged.addAll(left);
    }
    if (right != null) {
      merged.addAll(right);
    }
    return new ArrayList<>(merged);
  }

  public static final class MergeResult {

    private final IssueSearchFilters filters;
    private final boolean noMatch;

    public MergeResult(IssueSearchFilters filters) {
      this(filters, false);
    }

    private MergeResult(IssueSearchFilters filters, boolean noMatch) {
      this.filters = filters;
      this.noMatch = noMatch;
    }

    public IssueSearchFilters filters() {
      return filters;
    }

    public boolean isNoMatch() {
      return noMatch;
    }

    public static MergeResult conflicting() {
      return new MergeResult(IssueSearchFilters.empty(), true);
    }
  }
}
