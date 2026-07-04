package com.tenpo.challenge.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import com.tenpo.challenge.application.port.out.CallHistoryPersistencePort;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class RecordCallHistoryServiceTest {

  @Test
  void delegatesToPersistencePort() {
    RecordCallHistoryCommand command =
        new RecordCallHistoryCommand(
            Instant.parse("2026-07-01T22:15:30.123Z"),
            "GET",
            "/api/v1/calculations",
            null,
            null,
            "{\"result\":11}",
            null,
            200,
            true,
            10L,
            "127.0.0.1");
    RecordingPort port = new RecordingPort();

    new RecordCallHistoryService(port).execute(command).block();

    assertThat(port.command).isSameAs(command);
  }

  private static final class RecordingPort implements CallHistoryPersistencePort {

    private RecordCallHistoryCommand command;

    @Override
    public Mono<Void> save(RecordCallHistoryCommand cmd) {
      this.command = cmd;
      return Mono.empty();
    }
  }
}
