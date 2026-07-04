package com.tenpo.challenge.infrastructure.callhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import com.tenpo.challenge.application.port.in.RecordCallHistoryUseCase;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class AsyncCallHistoryRecorderTest {

  @Test
  void delegatesToUseCase() {
    AtomicReference<RecordCallHistoryCommand> captured = new AtomicReference<>();
    RecordCallHistoryUseCase useCase =
        cmd -> {
          captured.set(cmd);
          return Mono.empty();
        };
    AsyncCallHistoryRecorder recorder = new AsyncCallHistoryRecorder(useCase);
    RecordCallHistoryCommand command =
        new RecordCallHistoryCommand(
            Instant.parse("2026-07-01T22:15:30.123Z"),
            "POST",
            "/api/v1/calculations",
            null,
            "{\"num1\":5}",
            "{\"result\":11}",
            null,
            200,
            true,
            11L,
            "127.0.0.1");

    recorder.record(command).block();
    assertThat(captured.get()).isSameAs(command);
  }

  @Test
  void swallowsPersistenceFailures() {
    RecordCallHistoryUseCase useCase = cmd -> Mono.error(new IllegalStateException("boom"));
    AsyncCallHistoryRecorder recorder = new AsyncCallHistoryRecorder(useCase);
    RecordCallHistoryCommand command =
        new RecordCallHistoryCommand(
            Instant.parse("2026-07-01T22:15:30.123Z"),
            "POST",
            "/api/v1/calculations",
            null,
            "{\"num1\":5}",
            "{\"result\":11}",
            null,
            200,
            true,
            11L,
            "127.0.0.1");

    assertThatCode(() -> recorder.record(command).block()).doesNotThrowAnyException();
  }
}
