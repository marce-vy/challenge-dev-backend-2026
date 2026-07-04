package com.tenpo.challenge.persistence.callhistory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_history")
class CallHistoryEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "http_method", nullable = false, length = 10)
  private String httpMethod;

  @Column(name = "endpoint", nullable = false, length = 255)
  private String endpoint;

  @Column(name = "query_params", columnDefinition = "TEXT")
  private String queryParams;

  @Column(name = "request_body", columnDefinition = "TEXT")
  private String requestBody;

  @Column(name = "response_body", columnDefinition = "TEXT")
  private String responseBody;

  @Column(name = "error_body", columnDefinition = "TEXT")
  private String errorBody;

  @Column(name = "http_status", nullable = false)
  private int httpStatus;

  @Column(name = "success", nullable = false)
  private boolean success;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "client_ip", length = 64)
  private String clientIp;

  protected CallHistoryEntity() {}

  void setId(UUID id) {
    this.id = id;
  }

  void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  void setQueryParams(String queryParams) {
    this.queryParams = queryParams;
  }

  void setRequestBody(String requestBody) {
    this.requestBody = requestBody;
  }

  void setResponseBody(String responseBody) {
    this.responseBody = responseBody;
  }

  void setErrorBody(String errorBody) {
    this.errorBody = errorBody;
  }

  void setHttpStatus(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  void setSuccess(boolean success) {
    this.success = success;
  }

  void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }

  void setClientIp(String clientIp) {
    this.clientIp = clientIp;
  }

  UUID id() {
    return id;
  }

  Instant occurredAt() {
    return occurredAt;
  }

  String httpMethod() {
    return httpMethod;
  }

  String endpoint() {
    return endpoint;
  }

  String queryParams() {
    return queryParams;
  }

  String requestBody() {
    return requestBody;
  }

  String responseBody() {
    return responseBody;
  }

  String errorBody() {
    return errorBody;
  }

  int httpStatus() {
    return httpStatus;
  }

  boolean success() {
    return success;
  }

  Long durationMs() {
    return durationMs;
  }

  String clientIp() {
    return clientIp;
  }
}
