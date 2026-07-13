package com.sprinklr.sprintplanning.planning.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

final class PlanningAsyncSupport {

  private PlanningAsyncSupport() {
  }

  static <T> T joinFuture(CompletableFuture<T> future) {
    try {
      return future.join();
    } catch (CompletionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw ex;
    }
  }
}
