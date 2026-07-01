package com.sprinklr.sprintplanning.search.controller;

import com.sprinklr.sprintplanning.common.dto.ApiResponse;
import com.sprinklr.sprintplanning.search.dto.JqlReferenceDataDto;
import com.sprinklr.sprintplanning.search.dto.JqlSuggestionsDto;
import com.sprinklr.sprintplanning.search.service.JqlAutocompleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pods/{podId}/jql")
@Tag(name = "JQL Autocomplete", description = "Jira JQL field and value suggestions")
public class JqlAutocompleteController {

  private final JqlAutocompleteService jqlAutocompleteService;

  public JqlAutocompleteController(JqlAutocompleteService jqlAutocompleteService) {
    this.jqlAutocompleteService = jqlAutocompleteService;
  }

  @GetMapping("/reference")
  @Operation(summary = "Get JQL reference data for autocomplete (fields, operators, functions)")
  public ResponseEntity<ApiResponse<JqlReferenceDataDto>> getReferenceData(@PathVariable String podId) {
    return ResponseEntity.ok(ApiResponse.ok(jqlAutocompleteService.getReferenceData(podId)));
  }

  @GetMapping("/suggestions")
  @Operation(summary = "Get JQL value suggestions for a field")
  public ResponseEntity<ApiResponse<JqlSuggestionsDto>> getSuggestions(
      @PathVariable String podId,
      @RequestParam String fieldName,
      @RequestParam(required = false) String fieldValue,
      @RequestParam(required = false) String predicateName,
      @RequestParam(required = false) String predicateValue) {
    return ResponseEntity.ok(ApiResponse.ok(
        jqlAutocompleteService.getSuggestions(podId, fieldName, fieldValue, predicateName, predicateValue)));
  }
}
