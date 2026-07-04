package com.tenpo.challenge.persistence.callhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.callhistory.CallHistoryEntry;
import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CallHistoryPersistenceMapperTest {

  @Test
  void mapsEntityToApplicationEntry() {
    UUID id = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-07-01T22:15:30.123Z");
    CallHistoryEntity entity = new CallHistoryEntity();
    entity.setId(id);
    entity.setOccurredAt(occurredAt);
    entity.setHttpMethod("POST");
    entity.setEndpoint("/api/v1/calculations");
    entity.setQueryParams("a=1");
    entity.setRequestBody("{\"num1\":5,\"num2\":5}");
    entity.setResponseBody("{\"result\":11}");
    entity.setErrorBody(null);
    entity.setHttpStatus(200);
    entity.setSuccess(true);
    entity.setDurationMs(42L);
    entity.setClientIp("127.0.0.1");

    CallHistoryEntry entry = CallHistoryPersistenceMapper.toEntry(entity);

    assertThat(entry.id()).isEqualTo(id);
    assertThat(entry.occurredAt()).isEqualTo(occurredAt);
    assertThat(entry.httpMethod()).isEqualTo("POST");
    assertThat(entry.endpoint()).isEqualTo("/api/v1/calculations");
    assertThat(entry.queryParams()).isEqualTo("a=1");
    assertThat(entry.requestBody()).isEqualTo("{\"num1\":5,\"num2\":5}");
    assertThat(entry.responseBody()).isEqualTo("{\"result\":11}");
    assertThat(entry.errorBody()).isNull();
    assertThat(entry.httpStatus()).isEqualTo(200);
    assertThat(entry.success()).isTrue();
    assertThat(entry.durationMs()).isEqualTo(42L);
    assertThat(entry.clientIp()).isEqualTo("127.0.0.1");
  }

  @Test
  void mapsRecordCommandToEntity() {
    Instant occurredAt = Instant.parse("2026-07-01T22:15:30.123Z");
    RecordCallHistoryCommand command =
        new RecordCallHistoryCommand(
            occurredAt,
            "POST",
            "/api/v1/calculations",
            "a=1",
            "{\"num1\":5,\"num2\":5}",
            "{\"result\":11}",
            null,
            200,
            true,
            42L,
            "127.0.0.1");

    CallHistoryEntity entity = CallHistoryPersistenceMapper.toEntity(command);

    assertThat(entity.id()).isNotNull();
    assertThat(entity.occurredAt()).isEqualTo(occurredAt);
    assertThat(entity.httpMethod()).isEqualTo("POST");
    assertThat(entity.endpoint()).isEqualTo("/api/v1/calculations");
    assertThat(entity.queryParams()).isEqualTo("a=1");
    assertThat(entity.requestBody()).isEqualTo("{\"num1\":5,\"num2\":5}");
    assertThat(entity.responseBody()).isEqualTo("{\"result\":11}");
    assertThat(entity.errorBody()).isNull();
    assertThat(entity.httpStatus()).isEqualTo(200);
    assertThat(entity.success()).isTrue();
    assertThat(entity.durationMs()).isEqualTo(42L);
    assertThat(entity.clientIp()).isEqualTo("127.0.0.1");
  }
}
