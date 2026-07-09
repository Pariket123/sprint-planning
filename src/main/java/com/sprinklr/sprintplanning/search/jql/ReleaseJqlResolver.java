package com.sprinklr.sprintplanning.search.jql;

import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.search.FilterMergeHelper;
import com.sprinklr.sprintplanning.search.dto.IssueSearchFilters;
import com.sprinklr.sprintplanning.search.dto.IssueSearchReleaseRequest;
import com.sprinklr.sprintplanning.search.support.PodProjectKeyResolver;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ReleaseJqlResolver {

  private final JqlBuilder jqlBuilder;
  private final JqlMergeHelper jqlMergeHelper;
  private final FilterMergeHelper filterMergeHelper;
  private final PodProjectKeyResolver podProjectKeyResolver;

  public ReleaseJqlResolver(
      JqlBuilder jqlBuilder,
      JqlMergeHelper jqlMergeHelper,
      FilterMergeHelper filterMergeHelper,
      PodProjectKeyResolver podProjectKeyResolver) {
    this.jqlBuilder = jqlBuilder;
    this.jqlMergeHelper = jqlMergeHelper;
    this.filterMergeHelper = filterMergeHelper;
    this.podProjectKeyResolver = podProjectKeyResolver;
  }

  public Optional<String> resolve(
      PodDocument pod,
      ReleaseConfigDocument release,
      IssueSearchReleaseRequest request,
      JiraFieldConfig fieldConfig) {
    String baseJql = release.getBaseJql();
    if (baseJql != null && !baseJql.isBlank()) {
      String additionalJql = request != null ? request.additionalJql() : null;
      String mergedJql = jqlMergeHelper.merge(baseJql, additionalJql);
      if (mergedJql == null || mergedJql.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(mergedJql);
    }

    FilterMergeHelper.MergeResult mergeResult =
        filterMergeHelper.mergeWithRelease(release, IssueSearchFilters.empty());
    if (mergeResult.isNoMatch()) {
      return Optional.empty();
    }

    List<String> projectKeys = podProjectKeyResolver.resolveProjectKeys(List.of(pod));
    return jqlBuilder.build(projectKeys, mergeResult.filters(), fieldConfig);
  }
}
