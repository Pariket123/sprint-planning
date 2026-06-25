package com.sprinklr.sprintplanning.client.jira.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class JiraCredentialsStartupValidator implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(JiraCredentialsStartupValidator.class);

  private final JiraProperties jiraProperties;

  public JiraCredentialsStartupValidator(JiraProperties jiraProperties) {
    this.jiraProperties = jiraProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    boolean emailConfigured = jiraProperties.getEmail() != null && !jiraProperties.getEmail().isBlank();
    boolean tokenConfigured = jiraProperties.getApiToken() != null && !jiraProperties.getApiToken().isBlank();
    if (!emailConfigured || !tokenConfigured) {
      log.warn(
          "Jira credentials are not fully configured (email={}, token={}). "
              + "Set JIRA_EMAIL and JIRA_API_TOKEN or add them to .env in the project root.",
          emailConfigured ? "set" : "missing",
          tokenConfigured ? "set" : "missing");
    }
  }
}
