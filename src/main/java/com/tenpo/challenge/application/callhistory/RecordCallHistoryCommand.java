package com.tenpo.challenge.application.callhistory;

import java.time.Instant;
import java.util.Objects;

public record RecordCallHistoryCommand(
    Instant occurredAt,
    String httpMethod,
    String endpoint,
    String queryParams,
    String requestBody,
    String responseBody,
    String errorBody,
    int httpStatus,
    boolean success,
    Long durationMs,
    String clientIp) {

  public RecordCallHistoryCommand {
    Objects.requireNonNull(occurredAt, "occurredAt is required");
    requireText(httpMethod, "httpMethod is required");
    requireText(endpoint, "endpoint is required");
    Objects.requireNonNull(durationMs, "durationMs is required");
    if (durationMs < 0) {
      throw new IllegalArgumentException("durationMs must be greater than or equal to zero");
    }
    if (httpStatus < 100 || httpStatus > 599) {
      throw new IllegalArgumentException("httpStatus must be a valid HTTP status code");
    }
    if (success) {
      if (errorBody != null) {
        throw new IllegalArgumentException("errorBody must be null when success is true");
      }
    } else if (responseBody != null) {
      throw new IllegalArgumentException("responseBody must be null when success is false");
    }
  }

  private static void requireText(String value, String message) {
    Objects.requireNonNull(value, message);
    if (value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }
}
