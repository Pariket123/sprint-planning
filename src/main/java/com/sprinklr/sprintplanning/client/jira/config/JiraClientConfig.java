package com.sprinklr.sprintplanning.client.jira.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JiraProperties.class)
public class JiraClientConfig {

  @Bean
  public RestClient jiraHttpClient(JiraProperties properties) {
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
    requestFactory.setReadTimeout(Duration.ofSeconds(30));

    String credentials = properties.getEmail() + ":" + properties.getApiToken();
    String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    String baseUrl = trimTrailingSlash(properties.getBaseUrl());

    // #region agent log
    try (var fw = new java.io.FileWriter("/Users/pariket.pariket/project/.cursor/debug-0d262b.log", true)) {
      fw.write("{\"sessionId\":\"0d262b\",\"hypothesisId\":\"D\",\"location\":\"JiraClientConfig:jiraHttpClient\",\"message\":\"jira client config\",\"data\":{\"baseUrl\":\""
          + baseUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"emailConfigured\":"
          + (properties.getEmail() != null && !properties.getEmail().isBlank()) + ",\"tokenConfigured\":"
          + (properties.getApiToken() != null && !properties.getApiToken().isBlank())
          + "},\"timestamp\":" + System.currentTimeMillis() + "}\n");
    } catch (Exception ignored) {}
    // #endregion

    return RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .requestFactory(requestFactory)
        .build();
  }

  private static String trimTrailingSlash(String baseUrl) {
    if (baseUrl == null) {
      return "";
    }
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
