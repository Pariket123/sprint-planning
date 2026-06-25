package com.sprinklr.sprintplanning.common.exception;

import org.springframework.http.HttpStatus;

public class JiraClientException extends ApiException {

    public JiraClientException(String code, String message) {
        super(code, message, HttpStatus.BAD_GATEWAY);
    }

    public JiraClientException(String code, String message, Throwable cause) {
        super(code, message, HttpStatus.BAD_GATEWAY, cause);
    }

    public static JiraClientException unavailable(String message) {
        return new JiraClientException("JIRA_UNAVAILABLE", message);
    }

    public static JiraClientException unavailable(String message, Throwable cause) {
        return new JiraClientException("JIRA_UNAVAILABLE", message, cause);
    }

    public static JiraClientException badRequest(String message) {
        return new JiraClientException("JIRA_BAD_REQUEST", message);
    }

    public static JiraClientException unauthorized(String message) {
        return new JiraClientException("JIRA_UNAUTHORIZED", message);
    }
}
