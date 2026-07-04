package com.tenpo.challenge.application.callhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RecordCallHistoryCommandTest {

  @Test
  void acceptsSuccessfulResponseWithOnlyResponseBody() {
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

    assertThat(command.occurredAt()).isEqualTo(occurredAt);
    assertThat(command.responseBody()).isEqualTo("{\"result\":11}");
    assertThat(command.errorBody()).isNull();
  }

  @Test
  void acceptsErrorResponseWithOnlyErrorBody() {
    RecordCallHistoryCommand command =
        new RecordCallHistoryCommand(
            Instant.parse("2026-07-01T22:15:30.123Z"),
            "POST",
            "/api/v1/calculations",
            null,
            "{\"num1\":5}",
            null,
            "{\"status\":400}",
            400,
            false,
            15L,
            "127.0.0.1");

    assertThat(command.responseBody()).isNull();
    assertThat(command.errorBody()).isEqualTo("{\"status\":400}");
  }

  @Test
  void rejectsBodiesThatBreakSuccessClassificationRule() {
    assertThatThrownBy(
            () ->
                new RecordCallHistoryCommand(
                    Instant.parse("2026-07-01T22:15:30.123Z"),
                    "POST",
                    "/api/v1/calculations",
                    null,
                    null,
                    "{\"result\":11}",
                    "{\"status\":200}",
                    200,
                    true,
                    15L,
                    "127.0.0.1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("errorBody");
  }
}
