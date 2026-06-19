package com.sprinklr.sprintplanning.common.exception;

public class JiraRetryableException extends RuntimeException {

  public JiraRetryableException(String message, Throwable cause) {
    super(message, cause);
  }
}
