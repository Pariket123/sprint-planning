package com.sprinklr.sprintplanning.search.jql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JqlMergeHelperTest {

  private JqlMergeHelper jqlMergeHelper;

  @BeforeEach
  void setUp() {
    jqlMergeHelper = new JqlMergeHelper();
  }

  @Test
  void returnsBaseJqlWhenAdditionalIsBlank() {
    assertThat(jqlMergeHelper.merge("project = SCRUM", null))
        .isEqualTo("project = SCRUM");
    assertThat(jqlMergeHelper.merge("project = SCRUM", "  "))
        .isEqualTo("project = SCRUM");
  }

  @Test
  void mergesBaseAndAdditionalWithAnd() {
    assertThat(jqlMergeHelper.merge(
        "project = SCRUM AND fixVersion = \"Q3\"",
        "status = \"In Progress\""))
        .isEqualTo("(project = SCRUM AND fixVersion = \"Q3\") AND (status = \"In Progress\")");
  }

  @Test
  void stripsLeadingAndFromAdditionalJql() {
    assertThat(jqlMergeHelper.merge("project = SCRUM", "AND status = Open"))
        .isEqualTo("(project = SCRUM) AND (status = Open)");
  }

  @Test
  void returnsNullWhenBaseIsBlank() {
    assertThat(jqlMergeHelper.merge(null, "status = Open")).isNull();
    assertThat(jqlMergeHelper.merge("  ", "status = Open")).isNull();
  }
}
