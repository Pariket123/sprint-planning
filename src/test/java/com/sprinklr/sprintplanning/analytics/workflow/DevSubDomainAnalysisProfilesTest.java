package com.sprinklr.sprintplanning.analytics.workflow;

import com.sprinklr.sprintplanning.common.model.DevSubDomainIssueTypeProfile;
import com.sprinklr.sprintplanning.common.model.IssueView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DevSubDomainAnalysisProfilesTest {

  @Test
  void resolvesDefaultStoryProfileWhenKeyMissing() {
    DevSubDomainIssueTypeProfile profile =
        DevSubDomainAnalysisProfiles.resolveProfile(null, null);

    assertThat(profile.key()).isEqualTo("story");
    assertThat(profile.issueTypes()).containsExactly("Story");
  }

  @Test
  void filtersIssuesByConfiguredProfile() {
    DevSubDomainIssueTypeProfile profile =
        new DevSubDomainIssueTypeProfile("story", "Story", List.of("Story"));
    List<IssueView> issues = List.of(
        issue("SCRUM-1", "Story"),
        issue("SCRUM-2", "Bug"),
        issue("SCRUM-3", "Task"));

    List<IssueView> filtered = DevSubDomainAnalysisProfiles.filterIssues(issues, profile);

    assertThat(filtered).extracting(IssueView::key).containsExactly("SCRUM-1");
  }

  private static IssueView issue(String key, String issueType) {
    return new IssueView(key, "Summary", null, null, issueType, "To Do", null);
  }
}
