package com.tenpo.challenge.application.callhistory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CallHistoryEntry(
    UUID id,
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

  public CallHistoryEntry {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(occurredAt, "occurredAt is required");
    Objects.requireNonNull(httpMethod, "httpMethod is required");
    Objects.requireNonNull(endpoint, "endpoint is required");
    if (httpStatus < 100 || httpStatus > 599) {
      throw new IllegalArgumentException("httpStatus must be a valid HTTP status code");
    }
  }
}
