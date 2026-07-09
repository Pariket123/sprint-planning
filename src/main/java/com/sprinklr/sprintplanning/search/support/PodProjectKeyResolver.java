package com.sprinklr.sprintplanning.search.support;

import com.sprinklr.sprintplanning.team.model.PodDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PodProjectKeyResolver {

  public List<String> resolveProjectKeys(List<PodDocument> pods) {
    Set<String> projectKeys = new LinkedHashSet<>();
    for (PodDocument pod : pods) {
      if (pod.getJiraConfig() != null && pod.getJiraConfig().getProjectKeys() != null) {
        projectKeys.addAll(pod.getJiraConfig().getProjectKeys());
      }
    }
    return new ArrayList<>(projectKeys);
  }
}
