package com.sprinklr.sprintplanning.search.service;

import com.sprinklr.sprintplanning.search.dto.JqlReferenceDataDto;
import com.sprinklr.sprintplanning.search.dto.JqlSuggestionsDto;

public interface JqlAutocompleteService {

  JqlReferenceDataDto getReferenceData(String podId);

  JqlSuggestionsDto getSuggestions(
      String podId,
      String fieldName,
      String fieldValue,
      String predicateName,
      String predicateValue);
}
