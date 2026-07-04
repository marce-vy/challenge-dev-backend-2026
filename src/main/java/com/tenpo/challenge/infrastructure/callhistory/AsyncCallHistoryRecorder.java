package com.tenpo.challenge.infrastructure.callhistory;

import com.tenpo.challenge.api.callhistory.CallHistoryRecorder;
import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import com.tenpo.challenge.application.port.in.RecordCallHistoryUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

public class AsyncCallHistoryRecorder implements CallHistoryRecorder {

  private static final Logger log = LoggerFactory.getLogger(AsyncCallHistoryRecorder.class);

  private final RecordCallHistoryUseCase useCase;

  public AsyncCallHistoryRecorder(RecordCallHistoryUseCase useCase) {
    this.useCase = Objects.requireNonNull(useCase, "useCase is required");
  }

  @Async("callHistoryExecutor")
  public void record(RecordCallHistoryCommand command) {
    try {
      useCase.execute(command);
    } catch (RuntimeException ex) {
      log.warn("Could not persist call history record", ex);
    }
  }
}
