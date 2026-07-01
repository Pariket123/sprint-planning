package com.sprinklr.sprintplanning.client.jira;

import com.sprinklr.sprintplanning.client.jira.dto.JiraApproximateCountRequest;
import com.sprinklr.sprintplanning.client.jira.dto.JiraApproximateCountResponse;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueMoveRequest;
import com.sprinklr.sprintplanning.client.jira.dto.JiraJqlAutocompleteDataRequest;
import com.sprinklr.sprintplanning.client.jira.dto.JiraJqlReferenceDataDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraJqlSuggestionsDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraPagedResponse;
import com.sprinklr.sprintplanning.client.jira.dto.JiraProjectDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraSearchJqlRequest;
import com.sprinklr.sprintplanning.client.jira.dto.JiraSearchJqlResponse;
import com.sprinklr.sprintplanning.client.jira.dto.JiraSprintDto;
import com.sprinklr.sprintplanning.common.exception.JiraClientException;
import com.sprinklr.sprintplanning.common.exception.JiraRetryableException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class JiraRestClient {

  private static final int PAGE_SIZE = 50;
  private static final String FIELDS_PARAM = "summary,issuetype,status,statusCategory";
  private static final String SEARCH_FIELDS_PARAM =
      "summary,issuetype,status,statusCategory,assignee,priority,fixVersions,labels,components";

  private final RestClient restClient;
  private final RetryTemplate retryTemplate;

  public JiraRestClient(RestClient jiraHttpClient, RetryTemplate jiraRetryTemplate) {
    this.restClient = jiraHttpClient;
    this.retryTemplate = jiraRetryTemplate;
  }

  public List<JiraSprintDto> getBoardSprints(Long boardId, String state) {
    return retry(() -> {
      JiraPagedResponse<JiraSprintDto> response = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/rest/agile/1.0/board/{boardId}/sprint")
              .queryParam("state", state)
              .queryParam("maxResults", PAGE_SIZE)
              .build(boardId))
          .retrieve()
          .onStatus(this::isRetryable, this::throwRetryable)
          .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
            int status = clientResponse.getStatusCode().value();
            if (status == 401 || status == 403) {
              throw JiraClientException.unauthorized(
                  "Jira authentication failed. Set JIRA_EMAIL and JIRA_API_TOKEN (or add them to a .env file in the project root).");
            }
            throw JiraClientException.badRequest("Jira sprint list failed for board " + boardId);
          })
          .body(new ParameterizedTypeReference<JiraPagedResponse<JiraSprintDto>>() {});
      return response != null ? response.getValues() : List.of();
    });
  }

  public JiraSprintDto getSprint(Long sprintId) {
    return retry(() -> restClient.get()
        .uri("/rest/agile/1.0/sprint/{sprintId}", sprintId)
        .retrieve()
        .onStatus(this::isRetryable, this::throwRetryable)
        .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
          throw JiraClientException.badRequest("Jira sprint not found: " + sprintId);
        })
        .body(JiraSprintDto.class));
  }

  public List<JiraIssueDto> getSprintIssues(Long sprintId, List<String> extraFields) {
    return fetchAllIssues(uriBuilder -> uriBuilder
        .path("/rest/agile/1.0/sprint/{sprintId}/issue")
        .queryParam("fields", buildFieldsParam(extraFields))
        .build(sprintId));
  }

  public List<JiraIssueDto> getBacklogIssues(Long boardId, List<String> extraFields) {
    return fetchAllIssues(uriBuilder -> uriBuilder
        .path("/rest/agile/1.0/board/{boardId}/backlog")
        .queryParam("fields", buildFieldsParam(extraFields))
        .build(boardId));
  }

  public JiraPagedResponse<JiraIssueDto> getBacklogIssuesPage(
      Long boardId, List<String> extraFields, int startAt, int maxResults) {
    return retry(() -> restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/rest/agile/1.0/board/{boardId}/backlog")
            .queryParam("fields", buildFieldsParam(extraFields))
            .queryParam("startAt", startAt)
            .queryParam("maxResults", maxResults)
            .build(boardId))
        .retrieve()
        .onStatus(this::isRetryable, this::throwRetryable)
        .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
          throw JiraClientException.badRequest("Jira backlog fetch failed for board " + boardId);
        })
        .body(new ParameterizedTypeReference<JiraPagedResponse<JiraIssueDto>>() {}));
  }

  public void moveIssuesToSprint(List<String> issueKeys, Long sprintId) {
    retry(() -> {
      restClient.post()
          .uri("/rest/agile/1.0/sprint/{sprintId}/issue", sprintId)
          .body(new JiraIssueMoveRequest(issueKeys))
          .retrieve()
          .onStatus(this::isRetryable, this::throwRetryable)
          .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
            throw JiraClientException.badRequest("Failed to move issues to sprint " + sprintId);
          })
          .toBodilessEntity();
      return null;
    });
  }

  public void moveIssuesToBacklog(List<String> issueKeys) {
    retry(() -> {
      restClient.post()
          .uri("/rest/agile/1.0/backlog/issue")
          .body(new JiraIssueMoveRequest(issueKeys))
          .retrieve()
          .onStatus(this::isRetryable, this::throwRetryable)
          .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
            throw JiraClientException.badRequest("Failed to move issues to backlog");
          })
          .toBodilessEntity();
      return null;
    });
  }

  public JiraPagedResponse<JiraIssueDto> searchIssues(
      String jql, List<String> extraFields, int startAt, int maxResults) {
    List<String> fields = buildSearchFieldsList(extraFields);

    int remainingSkip = startAt;
    String nextPageToken = null;
    List<JiraIssueDto> collected = new ArrayList<>();
    boolean moreAvailable = false;
    boolean exhausted = false;

    while (collected.size() < maxResults && !exhausted) {
      int pageSize = remainingSkip > 0 ? PAGE_SIZE : maxResults - collected.size();
      JiraSearchJqlResponse page = executeJqlSearch(jql, fields, pageSize, nextPageToken);
      List<JiraIssueDto> issues = page != null && page.getIssues() != null ? page.getIssues() : List.of();
      if (issues.isEmpty()) {
        exhausted = true;
        break;
      }

      int index = 0;
      while (index < issues.size() && remainingSkip > 0) {
        index++;
        remainingSkip--;
      }
      while (index < issues.size() && collected.size() < maxResults) {
        collected.add(issues.get(index++));
      }

      if (collected.size() >= maxResults) {
        moreAvailable = index < issues.size() || (page != null && !page.isLast());
        break;
      }

      if (page == null || page.isLast()) {
        exhausted = true;
      } else {
        nextPageToken = page.getNextPageToken();
        if (nextPageToken == null || nextPageToken.isBlank()) {
          exhausted = true;
        }
      }
    }

    JiraPagedResponse<JiraIssueDto> response = new JiraPagedResponse<>();
    response.setStartAt(startAt);
    response.setMaxResults(maxResults);
    response.setTotal(getApproximateCount(jql));
    response.setLast(exhausted || !moreAvailable);
    response.setIssues(collected);

    return response;
  }

  public List<JiraIssueDto> getIssuesByKeys(List<String> issueKeys, List<String> extraFields) {
    if (issueKeys == null || issueKeys.isEmpty()) {
      return List.of();
    }
    String jql = buildIssueKeysJql(issueKeys);
    return searchAllIssues(jql, extraFields);
  }

  public List<JiraIssueDto> searchAllIssues(String jql, List<String> extraFields) {
    return fetchAllSearchResults(jql, extraFields);
  }

  public JiraJqlReferenceDataDto getJqlAutocompleteData(List<Long> projectIds) {
    if (projectIds == null || projectIds.isEmpty()) {
      return getJqlAutocompleteDataGet();
    }
    return getJqlAutocompleteDataPost(projectIds);
  }

  public JiraJqlSuggestionsDto getJqlAutocompleteSuggestions(
      String fieldName,
      String fieldValue,
      String predicateName,
      String predicateValue) {
    return retry(() -> restClient.get()
        .uri(uriBuilder -> {
          var builder = uriBuilder.path("/rest/api/3/jql/autocompletedata/suggestions");
          if (fieldName != null && !fieldName.isBlank()) {
            builder.queryParam("fieldName", fieldName);
          }
          if (fieldValue != null && !fieldValue.isBlank()) {
            builder.queryParam("fieldValue", fieldValue);
          }
          if (predicateName != null && !predicateName.isBlank()) {
            builder.queryParam("predicateName", predicateName);
          }
          if (predicateValue != null && !predicateValue.isBlank()) {
            builder.queryParam("predicateValue", predicateValue);
          }
          return builder.build();
        })
        .retrieve()
        .onStatus(this::isRetryable, this::throwRetryable)
        .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
          throw JiraClientException.badRequest("Jira JQL autocomplete suggestions failed");
        })
        .body(JiraJqlSuggestionsDto.class));
  }

  public JiraProjectDto getProject(String projectKeyOrId) {
    return retry(() -> restClient.get()
        .uri("/rest/api/3/project/{projectKeyOrId}", projectKeyOrId)
        .retrieve()
        .onStatus(this::isRetryable, this::throwRetryable)
        .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
          throw JiraClientException.badRequest("Jira project not found: " + projectKeyOrId);
        })
        .body(JiraProjectDto.class));
  }

  private JiraJqlReferenceDataDto getJqlAutocompleteDataGet() {
    return retry(() -> restClient.get()
        .uri("/rest/api/3/jql/autocompletedata")
        .retrieve()
        .onStatus(this::isRetryable, this::throwRetryable)
        .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
          throw JiraClientException.badRequest("Jira JQL autocomplete reference data failed");
        })
        .body(JiraJqlReferenceDataDto.class));
  }

  private JiraJqlReferenceDataDto getJqlAutocompleteDataPost(List<Long> projectIds) {
    return retry(() -> restClient.post()
        .uri("/rest/api/3/jql/autocompletedata")
        .body(new JiraJqlAutocompleteDataRequest(true, projectIds))
        .retrieve()
        .onStatus(this::isRetryable, this::throwRetryable)
        .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
          throw JiraClientException.badRequest("Jira JQL autocomplete reference data failed");
        })
        .body(JiraJqlReferenceDataDto.class));
  }

  private List<JiraIssueDto> fetchAllSearchResults(String jql, List<String> extraFields) {
    List<String> fields = buildSearchFieldsList(extraFields);
    List<JiraIssueDto> allIssues = new ArrayList<>();
    String nextPageToken = null;

    while (true) {
      JiraSearchJqlResponse page = executeJqlSearch(jql, fields, PAGE_SIZE, nextPageToken);
      if (page == null || page.getIssues() == null || page.getIssues().isEmpty()) {
        break;
      }
      allIssues.addAll(page.getIssues());
      if (page.isLast()) {
        break;
      }
      nextPageToken = page.getNextPageToken();
      if (nextPageToken == null || nextPageToken.isBlank()) {
        break;
      }
    }

    return allIssues;
  }

  private JiraSearchJqlResponse executeJqlSearch(
      String jql, List<String> fields, int maxResults, String nextPageToken) {
    return retry(() -> restClient.post()
        .uri("/rest/api/3/search/jql")
        .body(new JiraSearchJqlRequest(jql, maxResults, fields, nextPageToken))
        .retrieve()
        .onStatus(this::isRetryable, this::throwRetryable)
        .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
          throw JiraClientException.badRequest("Jira issue search failed");
        })
        .body(JiraSearchJqlResponse.class));
  }

  private int getApproximateCount(String jql) {
    try {
      JiraApproximateCountResponse response = retry(() -> restClient.post()
          .uri("/rest/api/3/search/approximate-count")
          .body(new JiraApproximateCountRequest(jql))
          .retrieve()
          .onStatus(this::isRetryable, this::throwRetryable)
          .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
            throw JiraClientException.badRequest("Jira approximate count failed");
          })
          .body(JiraApproximateCountResponse.class));
      return response != null ? response.getCount() : 0;
    } catch (JiraClientException ex) {
      return 0;
    }
  }

  private String buildIssueKeysJql(List<String> issueKeys) {
    String keysClause = issueKeys.stream()
        .map(this::escapeJqlString)
        .map(key -> "\"" + key + "\"")
        .reduce((left, right) -> left + "," + right)
        .orElse("");
    return "key in (" + keysClause + ")";
  }

  private String escapeJqlString(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private List<JiraIssueDto> fetchAllIssues(Function<UriBuilder, URI> uriFunction) {
    List<JiraIssueDto> allIssues = new ArrayList<>();
    int startAt = 0;
    boolean hasMore = true;

    while (hasMore) {
      final int pageStart = startAt;
      JiraPagedResponse<JiraIssueDto> page = retry(() -> restClient.get()
          .uri(uriBuilder -> {
            URI baseUri = uriFunction.apply(uriBuilder);
            return UriComponentsBuilder.fromUri(baseUri)
                .queryParam("startAt", pageStart)
                .queryParam("maxResults", PAGE_SIZE)
                .build(true)
                .toUri();
          })
          .retrieve()
          .onStatus(this::isRetryable, this::throwRetryable)
          .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
            throw JiraClientException.badRequest("Jira issue fetch failed");
          })
          .body(new ParameterizedTypeReference<JiraPagedResponse<JiraIssueDto>>() {}));

      if (page == null || page.getIssues().isEmpty()) {
        break;
      }
      allIssues.addAll(page.getIssues());
      startAt += page.getIssues().size();
      hasMore = !page.isLast() && startAt < page.getTotal();
    }

    return allIssues;
  }

  private String buildFieldsParam(List<String> extraFields) {
    if (extraFields == null || extraFields.isEmpty()) {
      return FIELDS_PARAM;
    }
    return FIELDS_PARAM + "," + String.join(",", extraFields);
  }

  private List<String> buildSearchFieldsList(List<String> extraFields) {
    List<String> fields = new ArrayList<>();
    for (String field : SEARCH_FIELDS_PARAM.split(",")) {
      fields.add(field);
    }
    if (extraFields != null) {
      for (String extraField : extraFields) {
        if (extraField != null && !extraField.isBlank() && !fields.contains(extraField)) {
          fields.add(extraField);
        }
      }
    }
    return fields;
  }

  private boolean isRetryable(HttpStatusCode status) {
    return status.is5xxServerError() || status.value() == 429;
  }

  private void throwRetryable(HttpRequest request, ClientHttpResponse response) throws IOException {
    throw new JiraRetryableException(
        "Retryable Jira error: HTTP " + response.getStatusCode(), null);
  }

  private <T> T retry(RetryCallback<T> callback) {
    try {
      return retryTemplate.execute(context -> callback.call());
    } catch (JiraClientException ex) {
      throw ex;
    } catch (JiraRetryableException ex) {
      throw JiraClientException.unavailable(ex.getMessage(), ex);
    } catch (RestClientResponseException ex) {
      if (isRetryable(ex.getStatusCode())) {
        throw JiraClientException.unavailable("Jira request failed: " + ex.getStatusText(), ex);
      }
      throw JiraClientException.badRequest("Jira request failed: " + ex.getMessage());
    } catch (Exception ex) {
      if (ex.getCause() instanceof JiraRetryableException retryable) {
        throw JiraClientException.unavailable(retryable.getMessage(), retryable);
      }
      throw JiraClientException.unavailable("Jira request failed", ex);
    }
  }

  @FunctionalInterface
  private interface RetryCallback<T> {
    T call();
  }
}
