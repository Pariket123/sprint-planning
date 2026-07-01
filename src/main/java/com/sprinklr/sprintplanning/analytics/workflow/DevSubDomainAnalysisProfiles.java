package com.sprinklr.sprintplanning.analytics.workflow;

import com.sprinklr.sprintplanning.common.model.DevSubDomainConfig;
import com.sprinklr.sprintplanning.common.model.DevSubDomainIssueTypeProfile;
import com.sprinklr.sprintplanning.common.model.IssueView;
import com.sprinklr.sprintplanning.common.model.JiraFieldConfig;
import com.sprinklr.sprintplanning.common.model.WorkflowAnalysisConfig;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class DevSubDomainAnalysisProfiles {

  public static final String DEFAULT_PROFILE_KEY = "story";

  private static final List<DevSubDomainIssueTypeProfile> DEFAULT_PROFILES = List.of(
      new DevSubDomainIssueTypeProfile("story", "Story", List.of("Story")),
      new DevSubDomainIssueTypeProfile("bug", "Bug", List.of("Bug", "Defect")));

  private DevSubDomainAnalysisProfiles() {
  }

  public static List<DevSubDomainIssueTypeProfile> effectiveProfiles(
      List<DevSubDomainIssueTypeProfile> configured) {
    if (configured == null || configured.isEmpty()) {
      return DEFAULT_PROFILES;
    }
    return configured;
  }

  public static DevSubDomainIssueTypeProfile resolveProfile(
      List<DevSubDomainIssueTypeProfile> configured,
      String profileKey) {
    List<DevSubDomainIssueTypeProfile> profiles = effectiveProfiles(configured);
    String resolvedKey = profileKey == null || profileKey.isBlank()
        ? DEFAULT_PROFILE_KEY
        : profileKey.trim();
    return profiles.stream()
        .filter(profile -> profile.key().equalsIgnoreCase(resolvedKey))
        .findFirst()
        .orElseGet(() -> profiles.getFirst());
  }

  public static Optional<DevSubDomainIssueTypeProfile> findAlternateProfile(
      List<DevSubDomainIssueTypeProfile> configured,
      String activeProfileKey) {
    List<DevSubDomainIssueTypeProfile> profiles = effectiveProfiles(configured);
    return profiles.stream()
        .filter(profile -> !profile.key().equalsIgnoreCase(activeProfileKey))
        .findFirst();
  }

  public static boolean matchesIssueType(String issueType, List<String> allowedTypes) {
    if (issueType == null || allowedTypes == null || allowedTypes.isEmpty()) {
      return false;
    }
    String normalized = issueType.toLowerCase(Locale.ROOT);
    return allowedTypes.stream()
        .anyMatch(type -> type != null && type.toLowerCase(Locale.ROOT).equals(normalized));
  }

  public static DevSubDomainIssueTypeProfile storyProfile(List<DevSubDomainIssueTypeProfile> configured) {
    return resolveProfile(configured, DEFAULT_PROFILE_KEY);
  }

  public static DevSubDomainIssueTypeProfile bugProfile(List<DevSubDomainIssueTypeProfile> configured) {
    return resolveProfile(configured, "bug");
  }

  public static List<DevSubDomainIssueTypeProfile> configuredProfiles(JiraFieldConfig fieldConfig) {
    WorkflowAnalysisConfig workflowConfig =
        fieldConfig != null ? fieldConfig.workflowAnalysis() : null;
    DevSubDomainConfig devSubDomainConfig =
        workflowConfig != null ? workflowConfig.devSubDomain() : null;
    return devSubDomainConfig != null ? devSubDomainConfig.issueTypeProfiles() : null;
  }

  public static List<IssueView> filterIssues(
      List<IssueView> issues,
      JiraFieldConfig fieldConfig,
      String profileKey) {
    WorkflowAnalysisConfig workflowConfig =
        fieldConfig != null ? fieldConfig.workflowAnalysis() : null;
    DevSubDomainConfig devSubDomainConfig =
        workflowConfig != null ? workflowConfig.devSubDomain() : null;
    List<DevSubDomainIssueTypeProfile> configuredProfiles = devSubDomainConfig != null
        ? devSubDomainConfig.issueTypeProfiles()
        : null;
    DevSubDomainIssueTypeProfile profile = resolveProfile(configuredProfiles, profileKey);
    return filterIssues(issues, profile);
  }

  public static List<IssueView> filterIssues(
      List<IssueView> issues,
      DevSubDomainIssueTypeProfile profile) {
    if (issues == null || issues.isEmpty() || profile == null) {
      return issues != null ? issues : List.of();
    }
    Set<String> allowedTypes = profile.issueTypes().stream()
        .map(type -> type.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
    if (allowedTypes.isEmpty()) {
      return issues;
    }
    return issues.stream()
        .filter(issue -> issue.issueType() != null
            && allowedTypes.contains(issue.issueType().toLowerCase(Locale.ROOT)))
        .toList();
  }
}
