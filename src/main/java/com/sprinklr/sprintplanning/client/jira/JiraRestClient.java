package com.sprinklr.sprintplanning.client.jira;

import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueDto;
import com.sprinklr.sprintplanning.client.jira.dto.JiraIssueMoveRequest;
import com.sprinklr.sprintplanning.client.jira.dto.JiraPagedResponse;
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
