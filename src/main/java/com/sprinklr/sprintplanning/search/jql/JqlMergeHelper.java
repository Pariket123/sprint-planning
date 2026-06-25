package com.sprinklr.sprintplanning.search.jql;

import org.springframework.stereotype.Component;

@Component
public class JqlMergeHelper {

  /**
   * Combines release base JQL with optional additional search JQL using AND.
   * Strips a leading "AND" from additional JQL when users paste clause fragments.
   */
  public String merge(String baseJql, String additionalJql) {
    String base = normalize(baseJql);
    if (base == null) {
      return null;
    }
    String additional = normalizeAdditional(additionalJql);
    if (additional == null) {
      return base;
    }
    return "(" + base + ") AND (" + additional + ")";
  }

  private String normalize(String jql) {
    if (jql == null || jql.isBlank()) {
      return null;
    }
    return jql.trim();
  }

  private String normalizeAdditional(String jql) {
    String trimmed = normalize(jql);
    if (trimmed == null) {
      return null;
    }
    if (trimmed.regionMatches(true, 0, "AND ", 0, 4)) {
      trimmed = trimmed.substring(4).trim();
    }
    return trimmed.isEmpty() ? null : trimmed;
  }
}
