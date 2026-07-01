package com.sprinklr.sprintplanning.search.controller;

import com.sprinklr.sprintplanning.TestSecurityConfig;
import com.sprinklr.sprintplanning.search.dto.JqlFieldReferenceDto;
import com.sprinklr.sprintplanning.search.dto.JqlReferenceDataDto;
import com.sprinklr.sprintplanning.search.dto.JqlSuggestionItemDto;
import com.sprinklr.sprintplanning.search.dto.JqlSuggestionsDto;
import com.sprinklr.sprintplanning.search.service.JqlAutocompleteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JqlAutocompleteController.class)
@Import({TestSecurityConfig.class, com.sprinklr.sprintplanning.common.handler.GlobalExceptionHandler.class})
class JqlAutocompleteControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JqlAutocompleteService jqlAutocompleteService;

  @Test
  void getReferenceDataReturnsEnvelope() throws Exception {
    when(jqlAutocompleteService.getReferenceData("pod-1"))
        .thenReturn(new JqlReferenceDataDto(
            List.of("and", "or"),
            List.of(new JqlFieldReferenceDto("status", "Status", null, List.of("=", "!="))),
            List.of()));

    mockMvc.perform(get("/api/v1/pods/pod-1/jql/reference"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.visibleFieldNames[0].value").value("status"));
  }

  @Test
  void getSuggestionsReturnsEnvelope() throws Exception {
    when(jqlAutocompleteService.getSuggestions(eq("pod-1"), eq("status"), eq("In"), isNull(), isNull()))
        .thenReturn(new JqlSuggestionsDto(
            List.of(new JqlSuggestionItemDto("In Progress", "In Progress"))));

    mockMvc.perform(get("/api/v1/pods/pod-1/jql/suggestions")
            .param("fieldName", "status")
            .param("fieldValue", "In"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.results[0].value").value("In Progress"));
  }
}
