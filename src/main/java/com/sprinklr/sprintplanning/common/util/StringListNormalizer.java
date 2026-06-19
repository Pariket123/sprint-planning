package com.sprinklr.sprintplanning.common.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StringListNormalizer {

  private StringListNormalizer() {
  }

  public static List<String> normalize(List<String> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    Set<String> normalized = new LinkedHashSet<>();
    for (String value : values) {
      if (value == null) {
        continue;
      }
      String trimmed = value.trim();
      if (!trimmed.isEmpty()) {
        normalized.add(trimmed);
      }
    }
    return new ArrayList<>(normalized);
  }
}
