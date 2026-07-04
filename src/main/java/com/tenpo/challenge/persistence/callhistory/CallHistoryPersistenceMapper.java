package com.tenpo.challenge.persistence.callhistory;

import com.tenpo.challenge.application.callhistory.CallHistoryEntry;
import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import java.util.Objects;
import java.util.UUID;

final class CallHistoryPersistenceMapper {

  private CallHistoryPersistenceMapper() {}

  static CallHistoryEntry toEntry(CallHistoryEntity entity) {
    return new CallHistoryEntry(
        entity.id(),
        entity.occurredAt(),
        entity.httpMethod(),
        entity.endpoint(),
        entity.queryParams(),
        entity.requestBody(),
        entity.responseBody(),
        entity.errorBody(),
        entity.httpStatus(),
        entity.success(),
        entity.durationMs(),
        entity.clientIp());
  }

  static CallHistoryEntity toEntity(RecordCallHistoryCommand command) {
    Objects.requireNonNull(command, "command is required");
    CallHistoryEntity entity = new CallHistoryEntity();
    entity.setId(UUID.randomUUID());
    entity.setOccurredAt(command.occurredAt());
    entity.setHttpMethod(command.httpMethod());
    entity.setEndpoint(command.endpoint());
    entity.setQueryParams(command.queryParams());
    entity.setRequestBody(command.requestBody());
    entity.setResponseBody(command.responseBody());
    entity.setErrorBody(command.errorBody());
    entity.setHttpStatus(command.httpStatus());
    entity.setSuccess(command.success());
    entity.setDurationMs(command.durationMs());
    entity.setClientIp(command.clientIp());
    return entity;
  }
}
