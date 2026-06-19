package com.sprinklr.sprintplanning.search;

import com.sprinklr.sprintplanning.release.model.ReleaseBasicFilters;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilterMergeHelperTest {

  private FilterMergeHelper filterMergeHelper;

  @BeforeEach
  void setUp() {
    filterMergeHelper = new FilterMergeHelper();
  }

  @Test
  void mergesReleaseFixVersionIncludesWithRequestFilters() {
    ReleaseConfigDocument release = releaseWith(
        "pod-1",
        List.of("Q3 2026", "Release-12.4"),
        List.of("Deprecated"),
        basicFilters(List.of("Story"), List.of("To Do"), List.of("BE"), List.of("High"), null));

    IssueSearchFilters request = new IssueSearchFilters(
        null, null, null, null,
        List.of("Q3 2026"),
        null, null, null, null, null, null, null);

    FilterMergeHelper.MergeResult result = filterMergeHelper.mergeWithRelease(release, request);

    assertThat(result.isNoMatch()).isFalse();
    assertThat(result.filters().fixVersions()).containsExactly("Q3 2026");
    assertThat(result.filters().fixVersionExcludes()).containsExactly("Deprecated");
  }

  @Test
  void returnsEmptyWhenReleaseAndRequestFixVersionsConflict() {
    ReleaseConfigDocument release = releaseWith(
        "pod-1",
        List.of("Q3 2026"),
        List.of(),
        basicFilters(null, null, null, null, null));

    IssueSearchFilters request = new IssueSearchFilters(
        null, null, null, null,
        List.of("Q4 2026"),
        null, null, null, null, null, null, null);

    FilterMergeHelper.MergeResult result = filterMergeHelper.mergeWithRelease(release, request);

    assertThat(result.isNoMatch()).isTrue();
  }

  @Test
  void doesNotConstrainFixVersionsWhenReleaseIncludesEmpty() {
    ReleaseConfigDocument release = releaseWith(
        "pod-1",
        List.of(),
        List.of(),
        basicFilters(null, null, null, null, null));

    IssueSearchFilters request = new IssueSearchFilters(
        null, null, null, null,
        List.of("Q3 2026"),
        null, null, null, null, null, null, null);

    FilterMergeHelper.MergeResult result = filterMergeHelper.mergeWithRelease(release, request);

    assertThat(result.isNoMatch()).isFalse();
    assertThat(result.filters().fixVersions()).containsExactly("Q3 2026");
  }

  @Test
  void intersectsBasicFilters() {
    ReleaseConfigDocument release = releaseWith(
        "pod-1",
        List.of(),
        List.of(),
        basicFilters(List.of("Story", "Bug"), List.of("To Do"), List.of("BE", "UI"), null, null));

    IssueSearchFilters request = new IssueSearchFilters(
        List.of("Story"), List.of("To Do"), List.of("BE"),
        null, null, null, null, null, null, null, null, null);

    FilterMergeHelper.MergeResult result = filterMergeHelper.mergeWithRelease(release, request);

    assertThat(result.isNoMatch()).isFalse();
    assertThat(result.filters().issueTypes()).containsExactly("Story");
    assertThat(result.filters().domains()).containsExactly("BE");
  }

  @Test
  void normalizesAndDeduplicatesFilterLists() {
    IssueSearchFilters filters = new IssueSearchFilters(
        List.of(" Story ", "Story"),
        null, null, null,
        List.of(" Q3 2026 ", "Q3 2026"),
        null, null, null, null, null, null, null);

    IssueSearchFilters normalized = filterMergeHelper.normalize(filters);

    assertThat(normalized.issueTypes()).containsExactly("Story");
    assertThat(normalized.fixVersions()).containsExactly("Q3 2026");
  }

  private ReleaseConfigDocument releaseWith(
      String podId,
      List<String> fixVersionIncludes,
      List<String> fixVersionExcludes,
      ReleaseBasicFilters basicFilters) {
    ReleaseConfigDocument release = new ReleaseConfigDocument();
    release.setPodId(podId);
    release.setFixVersionIncludes(fixVersionIncludes);
    release.setFixVersionExcludes(fixVersionExcludes);
    release.setBasicFilters(basicFilters);
    return release;
  }

  private ReleaseBasicFilters basicFilters(
      List<String> issueTypes,
      List<String> statuses,
      List<String> domains,
      List<String> priorities,
      List<String> assigneeIds) {
    ReleaseBasicFilters filters = new ReleaseBasicFilters();
    if (issueTypes != null) {
      filters.setIssueTypes(issueTypes);
    }
    if (statuses != null) {
      filters.setStatuses(statuses);
    }
    if (domains != null) {
      filters.setDomains(domains);
    }
    if (priorities != null) {
      filters.setPriorities(priorities);
    }
    if (assigneeIds != null) {
      filters.setAssigneeIds(assigneeIds);
    }
    return filters;
  }
}
