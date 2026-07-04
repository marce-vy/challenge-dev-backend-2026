package com.tenpo.challenge.persistence.callhistory;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("call_history")
public class CallHistoryEntity implements Persistable<UUID> {

  @Id
  @Column("id")
  private UUID id;

  @Transient private boolean isNew = true;

  @Column("occurred_at")
  private Instant occurredAt;

  @Column("http_method")
  private String httpMethod;

  @Column("endpoint")
  private String endpoint;

  @Column("query_params")
  private String queryParams;

  @Column("request_body")
  private String requestBody;

  @Column("response_body")
  private String responseBody;

  @Column("error_body")
  private String errorBody;

  @Column("http_status")
  private int httpStatus;

  @Column("success")
  private boolean success;

  @Column("duration_ms")
  private Long durationMs;

  @Column("client_ip")
  private String clientIp;

  public void setId(UUID id) {
    this.id = id;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public void setQueryParams(String queryParams) {
    this.queryParams = queryParams;
  }

  public void setRequestBody(String requestBody) {
    this.requestBody = requestBody;
  }

  public void setResponseBody(String responseBody) {
    this.responseBody = responseBody;
  }

  public void setErrorBody(String errorBody) {
    this.errorBody = errorBody;
  }

  public void setHttpStatus(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }

  public void setClientIp(String clientIp) {
    this.clientIp = clientIp;
  }

  public UUID id() {
    return id;
  }

  public Instant occurredAt() {
    return occurredAt;
  }

  public String httpMethod() {
    return httpMethod;
  }

  public String endpoint() {
    return endpoint;
  }

  public String queryParams() {
    return queryParams;
  }

  public String requestBody() {
    return requestBody;
  }

  public String responseBody() {
    return responseBody;
  }

  public String errorBody() {
    return errorBody;
  }

  public int httpStatus() {
    return httpStatus;
  }

  public boolean success() {
    return success;
  }

  public Long durationMs() {
    return durationMs;
  }

  public String clientIp() {
    return clientIp;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }
}
