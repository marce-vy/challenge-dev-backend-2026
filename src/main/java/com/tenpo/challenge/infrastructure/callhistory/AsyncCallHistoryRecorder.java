package com.tenpo.challenge.infrastructure.callhistory;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import com.tenpo.challenge.application.port.in.RecordCallHistoryUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class AsyncCallHistoryRecorder implements CallHistoryRecorder {

  private static final Logger log = LoggerFactory.getLogger(AsyncCallHistoryRecorder.class);

  private final RecordCallHistoryUseCase useCase;

  public AsyncCallHistoryRecorder(RecordCallHistoryUseCase useCase) {
    this.useCase = Objects.requireNonNull(useCase, "useCase is required");
  }

  @Override
  public Mono<Void> record(RecordCallHistoryCommand command) {
    return useCase
        .execute(command)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(
            RuntimeException.class,
            ex -> {
              log.warn("Could not persist call history record", ex);
              return Mono.empty();
            });
  }
}
